## http://www.ffmpeg.org/download.html

ffmpeg -i $1 -y -fs 512k -vcodec h264 -s 256x256 -q:v $2 -r:v $3 $4

