#!/bin/bash

set -e

CRF_FACTOR=18
TRANSCODE_SUFFIX="-tc"
INPUT_EXTENSION="mp4"
OUTPUT_EXTENSION="mp4"
PRESET="medium"
PIX_FMT="yuv420p"
FFMPEG_EXTRA=""

# Keep track of how much we've processed
count=0
remaining=0
total_files=0
skipped=0
total_bytes_saved=0
mb_saved_total=0
total_bytes_processed=0

# The current file we're processing
input_filename=""
output_filename=""
just_filename=""
input_byte_size=0
output_byte_size=0

BACKUP_METADATA=false # If set, dumps a text file with all the metadata of the input file before converting

# If we're converting video from a GoPro, do extra to preserve the metadata (gps and sensor data) track
# GoPro software (e.g. Quik) looks for the the go pro handler names, if not present it will not see the video as it
# thinks it is from a non-gopro camera. By updating the handler names to what it is expecting, we can trick it into
# recognising the videos to be GoPro content and editable in Quik. To enable the gauges (gps, sensor data) Quik is even
# more picky, and must find the 'gpmd' data track inside the video, with a handler name starting with a tab!
# For consistency, we map all the handler names with a tab as that seems to be what the GoPro itself spits out
# -tag:d:2 'gpmd' => This is effectively a hack to force ffmpeg to copy the binary 'fdsc' stream, which is present in the
# original go pro files. Not sure exactly what it is for, but would rather keep it just in case. Unfortunately ffmpeg does Not
# understand this handler type, so spits out the message: '[mp4 @ 0x7fed5a80cc00] Unknown hldr_type for fdsc, writing dummy values'
# This effectively just adds a useless dummy stream. Using this hack we instead get the original stream copied, by telling it the
# the handler_type it should use is actually 'gpmd'. Since we're just copying it as is and not manipulating it this should be fine.
# Then to be able to distinguish it from the *real* gpmd stream, we change the handler name in the metadata to be GoPro SOS (fdsc stream)
GOPRO_TRANSCODING=false
GOPRO_TRANSCODING_HERO4=false
AUDIO_MODE=false
AUDIO_AND_VIDEO_MODE=false
DRONE_MODE=false

FFMPEG_OPTIONS_GOPRO_MAP='-map 0:v -map 0:a -map 0:m:handler_name:"	GoPro TCD" -map 0:m:handler_name:"	GoPro MET" -map 0:m:handler_name:"	GoPro SOS" -tag:d:1 "gpmd" -tag:d:2 "gpmd"'
FFMPEG_GOPRO_STD_METADATA='-metadata:s:v handler="	GoPro AVC" -metadata:s:a handler="	GoPro AAC" '
FFMPEG_OPTIONS_GOPRO_METADATA="${FFMPEG_GOPRO_STD_METADATA}"' -metadata:s:d:0 handler="	GoPro TCD" -metadata:s:d:1 handler="	GoPro MET" -metadata:s:d:2 handler="	GoPro SOS (original fdsc stream)"'
FFMPEG_OPTIONS_AUDIO="-codec:a libfdk_aac -vbr 4"

usage() { echo "Usage: shrinkwrap [-c :crf -i :inputextension -b -g -g] <file>" 1>&2;ffmpeg exit 0; }

# Simple log, write to stdout
log () {
  echo ">>>> `date -u`: $1" | tee -a $LOG_LOCATION
}

shrink_video () {
  # -copy_unknown => if there are streams ffmpeg doesn't know about, still copy them (e.g some GoPro data stuff)
  # -map_metadata => copy over the global metadata from the first (only) input
  # -map 0 => copy *all* streams found in the file, not just the best audio and video as is the default (e.g. including data)
  # -c copy => for all streams, default to just copying as it with no transcoding
  # -codec:v libx264 -pix_fmt $PIX_FMT -crf $CRF_FACTOR => specifically for the video stream, reencode using x264 and the specified pix_fmt and crf factor
  FFMPEG_OPTIONS_COMMON="-copy_unknown -map_metadata 0 -c copy -codec:v libx264 -crf $CRF_FACTOR -preset $PRESET $FFMPEG_EXTRA"

  # Check if we should transcode the audio in the video too
  if [ "$AUDIO_AND_VIDEO_MODE" = true ] ; then FFMPEG_OPTIONS_COMMON="$FFMPEG_OPTIONS_COMMON $FFMPEG_OPTIONS_AUDIO"; fi

  FFMPEG_OPTIONS_STANDARD="-map 0 -pix_fmt $PIX_FMT"

  if [ "$GOPRO_TRANSCODING" = true ] && [ "$GOPRO_TRANSCODING_HERO4" = false ] ; then
    local cmd_ffmpeg="ffmpeg -noautorotate -i \"$input_filename\" $FFMPEG_OPTIONS_COMMON -pix_fmt yuvj420p $FFMPEG_OPTIONS_GOPRO_MAP $FFMPEG_OPTIONS_GOPRO_METADATA \"$output_filename\""
  else
		if [ "$GOPRO_TRANSCODING_HERO4" = true ] ; then
			local cmd_ffmpeg="ffmpeg -noautorotate -i \"$input_filename\" $FFMPEG_OPTIONS_COMMON $FFMPEG_OPTIONS_STANDARD $FFMPEG_GOPRO_STD_METADATA \"$output_filename\""
		else
			if [ "$DRONE_MODE" = true ] ; then
				# for drones we reencode the video to h264 but also the audio to aac
				local cmd_ffmpeg="ffmpeg -noautorotate -i \"$input_filename\" $FFMPEG_OPTIONS_COMMON $FFMPEG_OPTIONS_STANDARD $FFMPEG_OPTIONS_AUDIO \"$output_filename\""
			else
				# If we get here, its just a standard video from phone or camera
				local cmd_ffmpeg="ffmpeg -noautorotate -i \"$input_filename\" $FFMPEG_OPTIONS_COMMON $FFMPEG_OPTIONS_STANDARD \"$output_filename\""
			fi
		fi
  fi

  log "Executing $cmd_ffmpeg"
  eval "$cmd_ffmpeg"
}

shrink_audio () {
  local cmd_ffmpeg="ffmpeg -i \"$input_filename\" -copy_unknown -map_metadata 0 -map 0 $FFMPEG_OPTIONS_AUDIO \"$output_filename\""

  log "Executing $cmd_ffmpeg"
  eval "$cmd_ffmpeg"
}

recover_metadata () {
  local cmd_exiftool="exiftool -tagsfromfile \"$input_filename\" -extractEmbedded -all:all -"*gps*" -time:all --FileAccessDate --FileInodeChangeDate -FileModifyDate -ext $OUTPUT_EXTENSION -overwrite_original \"$output_filename\""
  log "Executing $cmd_exiftool"
  eval "$cmd_exiftool"
}

update_counters () {
  (( count++ ))
  (( total_bytes_processed+=$input_byte_size ))
  output_byte_size=$(stat -f%z "$output_filename")
  bytes_saved=$((input_byte_size - output_byte_size))
  (( total_bytes_saved+=$bytes_saved ))
}

show_results () {
  mb_processed=$((total_bytes_processed >> 20))
  mb_output=$((output_byte_size >> 20))
  mb_saved=$((bytes_saved >> 20))
  percent_of_original=$(bc -l <<< "scale=2;$output_byte_size/$input_byte_size")
  percent_reduced=$(bc -l <<< "100-($percent_of_original*100)")
  echo $percent_of_original
  mb_saved_total=$((total_bytes_saved >> 20))
  msg="($count/$total_files) Processed ${mb_processed}MB, shrinkwrapped to ${mb_output}MB. Saved another ${mb_saved}MB (reduced by: ${percent_reduced}%) (Total ${mb_saved_total}MB saved)"
  log "${msg}"
  #say "Saved another ${mb_saved}MB! That's ${mb_saved_total}MB now!"
}

backup_metadata () {
  log "Backuping up metadata for $input_filename"
  local metadata_file="${just_filename}_metadata.txt"
  local cmd_backup_metadata="exiftool -s -ExtractEmbedded $input_filename > ${metadata_file}"
  log "Executing ${cmd_backup_metadata}"
  eval "${cmd_backup_metadata}"
}

shrinkwrap () {
  input_filename="$@"

  log "Input: $input_filename"

  extension="${input_filename##*.}"
  just_filename="${input_filename%.*}"
  input_byte_size=$(stat -f%z "$input_filename")
  output_filename="${just_filename}${TRANSCODE_SUFFIX}.${OUTPUT_EXTENSION}"

  if [ ! -e "$output_filename" ]; then
    log "shrinkwrapping file $1"
    log "output filename: $output_filename"

    # Process the audio/video
		if [ "$AUDIO_MODE" = true ] ; then
    	shrink_audio
		else
			shrink_video
		fi

    recover_metadata
    if [ "$BACKUP_METADATA" = true ] ; then
      backup_metadata
    fi
    update_counters
    show_results
  else
    log "skipping file $input_filename (a transcoded version already exists)"
  fi
}

while getopts c:e:i:f:p:aAbdgh opt ; do
  case $opt in
		a)
			INPUT_EXTENSION="m4a"
			OUTPUT_EXTENSION="m4a"
			AUDIO_MODE=true
			log "running in audio mode: ON (input/output extension set to .m4a)"
			;;
    A)
			AUDIO_AND_VIDEO_MODE=true
			log "transcoding video audio: ON"
			;;
		b)
      BACKUP_METADATA=true
      log "backup of original metadata: ON"
      ;;
		c)
      CRF_FACTOR=${OPTARG}
      log "crf factor set to $CRF_FACTOR"
      ;;
		d)
			DRONE_MODE=true
      PIX_FMT="yuvj420p"
			log "running in drone mode: ON"
			;;
    e)
      FFMPEG_EXTRA=${OPTARG}
      log "adding extra opts for ffmpeg: $FFMPEG_EXTRA"
      ;;
		h)
      GOPRO_TRANSCODING_HERO4=true
      log "gopro hero 4 transcoding: ON"
      ;;
		i)
			INPUT_EXTENSION="${OPTARG}"
			log "input extension set to ${INPUT_EXTENSION}. Only considering files of this type"
			;;
    f)
			PIX_FMT="${OPTARG}"
			log "pxl_fmt set to $PIX_FMT"
			;;
    g)
      GOPRO_TRANSCODING=true
      log "gopro specific transcoding: ON"
      ;;
		p)
			PRESET="${OPTARG}"
			log "preset set to ${PRESET}"
			;;
    \?)
      usage
      exit 0
      ;;
  esac
done

# Get the file we want to shrinkwrap
shift $(( OPTIND-1 ))

if [[ -d $1 ]]; then
  log "Shrinkwrapping directory: $1"

  # Loop through each file in the directory, shrinkwrapping each one
  total_files=$(find . -type f -maxdepth 1 -iname "*.${INPUT_EXTENSION}" | grep -v -- "$TRANSCODE_SUFFIX" | wc -l | awk '{$1=$1};1')
  remaining=$total_files
  log "$total_files files to process"

	shopt -s nocaseglob #allow case insensitive match below
  for f in $1/*.${INPUT_EXTENSION} ; do
    if [[ $f = *"$TRANSCODE_SUFFIX"* ]]; then
      log "skipping file $escaped_file (looks like its already been transcoded)"
      (( skipped++ ))
    else
      shrinkwrap "$f"
      (( remaining-- ))
      log "$remaining files remaining"
    fi
  done
else
  log "Shrinkwrapping single file: $1"
  total_files=1
  remaining=1
  shrinkwrap $1
fi

msg="FINISHED. Transcoded $count files (skipped $skipped), saved ${mb_saved_total}MB!"
log "$msg"
#say "$msg"
