rem @echo off
@rem http://www.ffmpeg.org/download.html

"C:\Program Files\ffmpeg\bin\ffmpeg.exe" -i %1 -y -fs 512k -vcodec h264 -s 256x256 -q:v %2 -r:v %3 %4

@rem best rate - mpeg4 but not see in CHROME
@rem "C:\Program Files\ffmpeg\bin\ffmpeg.exe" -i 0> -y -fs 200k -vcodec mpeg4 -q:v 20 -r:v 10 -s 255x255 dataPreviews\probe.mp4

rem for UNIX stdin - use pipe
rem see list of protocols:
rem "C:\Program Files\ffmpeg\bin\ffmpeg.exe" -protocols

rem "C:\Program Files\ffmpeg\bin\ffmpeg.exe" -codecs

rem "C:\Program Files\ffmpeg\bin\ffmpeg.exe" -i %1 -y -fs 200k -vcodec mpeg4 -q:v 20 -r:v 10 -s 255x255 %2

