# shrinkwrap

Shrinkwrap is a simple tool to shrink (reduce the file size) of audio and video files 
while preserving as much of the original metadata as possible.

**Problem**:

You shoot loads of videos with your phone, camera, GoPro etc and over time the amount
of disk space used grows considerably.

You'd like to reduce the size of some of these files but also:

 * Keep them at a nice quality (where you can't notice the difference)
 * Keep the original metadata (especially for GoPro video, make
 sure it still has the write tracks inside to work with GoPro software like Quik)
 * Keep the original file modification timestamps so you can still sort by date and stuff
 
Basically, you want the files to be as similar as possible as the original, just a bit...
smaller.

This is the goal of Shrinkwrap.

## Quick Start

The simplest way to use Shrinkwrap is via [docker](https://www.docker.com/get-started):

Just run something like:

```
docker run -v /path/to/your/files:/files bennetimo/shrinkwrap \
 --input-extension mp4 /files

```

Where:
 
 * `/path/to/your/files` is a directory containing the files you would like to shrink.
 * `--input-extension mp4` is the type of files to consider (anything else will be ignored)
 
> It's also possible to shrink a single file instead of a directory. To do that, just change the last `/files`
above to `/files/yourfile.mp4`, to get something like:
 `docker run -v /path/to/your/files:/files bennetimo/shrinkwrap --input-extension mp4 /files/yourfile.mp4`
 
Running this command will fire up shrinkwrap and set it to work on the files you specified. 

> By default, it will transcode video to h264 using the libx264 codec, and audio to 
aac using libfdk_aac

Each eligible file will be transcoded using the standard preset, and written to a new file in
the same directory of the original, with the suffix `'-tc'`. 

Now check the transcoded file for quality etc. If it meets your needs you can delete
the original and just keep the transcode. If not, tweak the settings to increase/decreate quality
or file size and various other settings.
  
> See below for more customisation.

## Process

When Shrinkwrap runs, this is the steps it takes:

 1. Builds a list of all the files that you specified (looking into each specified directory also)
 2. Filter the list to find appropriate files to shrink 
 (files of the wrong type, or already shrinkwrapped files are ignored)
 3. Transcodes the audio/video files using [ffmpeg](https://www.ffmpeg.org/) 
 4. Recovers file modification times of the files using [exiftool](https://www.sno.phy.queensu.ca/~phil/exiftool/)

> The original files are not touched, instead new transcoded versions of each
file are created in the same directory

## Customisation

You can specify different options to control how and what is transcoded:


| Option  | Long Option | Default       | Meaning           |
| ------- | ----------- | ------------- | ----------------- | 
| -i | --input-extension | N/A | The type of files to transcode |
| -o | --output-extension | mp4 | output file format (e.g. mp4, aac) |
| -a | --audio | true | whether to transcode audio or copy |
| -v | --video | true | whether to transcode video or copy |
| -p | --preset | standard | preset to use (standard, gopro4, gopro5, gopro7) |
| -b | --backup-metadata | false | whether to dump each original files metadata to a text file |
| -s | --transcode-suffix | -tc | suffix used to identify a transcoded file |
| -f | --force-overwrite | false | if true, will retranscode already transcoded files |
| N/A | --ffmpeg-opts k1=v1,k2=v2 | N/A | Arbitrary ffmpeg options to add for the transcode |

For example, if you wanted to transcode GoPro Hero5 video files at an ffmpeg
crf quality setting of 20 using the speed preset veryfast, you might use something like:

```
docker run -v /path/to/your/files:/files bennetimo/shrinkwrap:latest \
    --input-extension mp4 --ffmpeg-opts crf=20,preset=veryfast --preset gopro5 /files
```

## Presets

There are 4 presets available (submit a PR to add more if you want). Each preset is basically a collection
of options to pass to ffmpeg. There is a huge and extensive [documentation](https://www.ffmpeg.org/ffmpeg.html)
of all the configuration you can do, so if you're not familiar with ffmpeg that's a good place to start.
 
At the moment each preset is specified as a map of ffmpeg keys and arguments. 
 
#### standard

Encodes video to h264 and audio to aac, using fairly default ffmpeg options

```
"copy_unknown" -> "",           //if there are streams ffmpeg doesn't know about, still copy them (e.g some GoPro data stuff)
"map_metadata" -> "0",          //copy over the global metadata from the first (only) input
"map"          -> "0",          //copy *all* streams found in the file, not just the best audio and video as is the default (e.g. including data)
"codec"        -> "copy",       //for all streams, default to just copying as it with no transcoding
"preset"       -> "medium"      //fmpeg speed preset to use

"codec:v" -> "libx264",         //specifically for the video stream, reencode to x264
"pix_fmt" -> "yuv420p",         //default pix_fmt
"crf"     -> "23"               //default constant rate factor for quality. 0-52 where 18 is near visually lossless

"codec:a" -> "libfdk_aac",      //specifically for the audio stream, reencode to aac
"vbr"     -> "4"                //variable bit rate quality setting
```
 
#### gopro4

Same as standard, except it names the video and audio tracks to match what GoPro software expects

```
"metadata:s:v:" -> "handler='\tGoPro AVC'",
"metadata:s:a:" -> "handler='\tGoPro AAC'"
```

#### gopro5

Builds on standard and tries to retain as much of the hero5 metadata as possible. 

The GoPro Hero 5 uses a handler name that starts with a tab character (yes, you read that correct!), and GoPro
software will not detect any files unless the handler names match exactly.

This preset picks out each of the streams from the files generated by the Hero5 by name so that the order can
be preserved in the transcode file, and then each of the streams is renamed on output to include the tab again.

> Note ffmpeg does not recognise the 'fdsc' stream as it is non-standard, so we do a little hack to tag it as
'gpmd' and then copy it across with no modification. Otherwise, ffmpeg will just use dummy values for it. This
stream doesn't seem to be very important, but might as well try to keep it if we can. 
See [here](https://www.reddit.com/r/ffmpeg/comments/8qosoj/merging_raw_gpmd_as_metadata_stream/) for more discussion.

```
"pix_fmt"                            -> "yuvj420p",
"map 0:v"                            -> "",
"map 0:a"                            -> "",
"map 0:m:handler_name:'\tGoPro TCD'" -> "",
"map 0:m:handler_name:'\tGoPro MET'" -> "",
"map 0:m:handler_name:'\tGoPro SOS'" -> "",
"tag:d:1"                            -> "'gpmd'",
"tag:d:2"                            -> "'gpmd'",
"metadata:s:v:"                      -> "handler='\tGoPro AVC'",
"metadata:s:a:"                      -> "handler='\tGoPro AAC'",
"metadata:s:d:0"                     -> "handler='\tGoPro TCD'",
"metadata:s:d:1"                     -> "handler='\tGoPro MET'",
"metadata:s:d:2"                     -> "handler='\tGoPro SOS (original fdsc stream)'"
```
 
#### gopro7

Same as gopro5 but takes into account stream name changes. 

## Dependencies

 * [ffmpeg](https://www.ffmpeg.org/) 
 * [exiftool](https://www.sno.phy.queensu.ca/~phil/exiftool/)
