# Lumix Photobox

This fork offers a simple photobox user interface to be interacted with via touchscreen and uses a Lumix camera. 

It is based on an fork of an unofficial counterpart of the official [Panasonic Lumix Link mobile app](https://play.google.com/store/apps/details?id=jp.co.panasonic.lumix_link.activity&hl=cs). With this application, you can remotely control your Lumix camera, take pictures, record video and adjust capture settings. 

# Dependencies

for the live view to work, you need :
- Java RE
- Add the java bin directory to your PATH


## Usage instructions

- start wifi hotspot on your camera
- connect your computer to the camera's hotspot
- start control.bat
- enter your cameras IP address (for hotspot mode 192.168.54.1)
- press the button to take a photo

When you first connect the app to the camera, the camera will ask you if you want to connect "Lumix Link Desktop". You have to agree here.
If this information does not show automatically you may get an error like like _err-unsuitable-app_ instead.
Open `http://CAMERA_IP/cam.cgi?mode=accctrl&type=req_acc&value=0&value2=Vexia%20Fcs` in your browser replacing `CAMERA_IP` by the IP address of your camera,

You can check the status of the connection by opening `http://192.168.54.1/cam.cgi?mode=getstate`.

## Supported models

The basic parts of this application should work with all Lumix cameras that can be used with the mobile app. 

## Origin of this application

This is a "fork" of a "fork" of the application published on http://www.personal-view.com/talks/discussion/6703/control-your-gh3-from-a-web-browser-now-with-video-/p1 . Unfortunately, I haven't found a repository for this application, so I had to create my own one and now I'm trying to communicate with the original author to merge our efforts.

Thanks very much to leniusible for the initial work on reverse-engeneering the communication protocol. If you are lenuisible, please, contact me here on github!

## License

The license of the original application is unclear. I assumed it is given to the public without any restrictions.

Changes since the first fork are covered by the 3-clause BSD license.
