======
INDEX:
======

1. Builds
2. Command Line Options
3. Applets
4. NE2000 Ethernet

==========
1. Builds:
==========

jdosbox.jar 

  Standard build.  Good for most people.  Includes the recompiler and references no native code.

jdosbox_applet.jar

  Lite build.  INcludes everything from jdosbox.jar except for the recompiler.  The recompiler
  would not work in unsigned applets anyway, so no reason to include it.  For signed applets
  go ahead and use the standard build since it will be faster

jdosbox_pcap.jar

  Ethernet build.  This includes experimental support for the NE2000 ethernet card via native pcap
  library.

========================
2. Command Line Options:
========================

-compile <name>

  Note: Not availabe in jdosbox_applet.jar
  
  Example: java -jar jdosbox.jar -compile doom
  
  Result: This will produce a file called doom.jar in the current directory with blocks that were
  compiled while you ran the program.

  See the [compiler] section in the config file for options to fine tune what gets compiled.

-fullscreen

  Starts the app in full screen mode

-pcap
-pcapport

  See NE2000 Ethernet section

===========
3. Applets:
===========


** Use an image from within a JAR.  

   Pro: This works well for signed and unsigned applets. 
   Cons: A little slower and read-only.  No save games.

   To create an image I would recommend using Dosbox Megabuild at http://home.arcor.de/h-a-l-9000/

   <APPLET CODE="jdos.gui.MainApplet" archive='jdosbox.jar,doom.jar' WIDTH=640 HEIGHT=400>
    <param name="param1" value="imgmount e jar://doom.img -size 512,16,2,512"">
    <param name="param2" value="e:">
    <param name="param3" value="doom">
   </APPLET> 

   More Info: Make sure you put the image file inside a sub directory called jdos.  This file may
   have the extension either jar or zip.

** Download, unzip files to client machine

   Pro: This allows save games to works
   Con: The applets must be signed (can be self signed)

   <APPLET CODE="jdos.gui.MainApplet" archive='jdosbox.jar' WIDTH=640 HEIGHT=400>
    <param name="download1" value="http://jdosbox.sourceforge.net/doom.04.zip">
    <param name="param1" value="imgmount e ~/.jdosbox/doom.04/doom.img -size 512,16,2,512">
    <param name="param2" value="e:">
    <param name="param3" value="doom">
    <param name="param4" value="-conf ~/.jdosbox/doom.04/dosbox.conf">
   </APPLET>

   More Info: ~ folder is the home directory for the user.  On Windows this might be c:\users\name\
   jDosbox will download the zip mentioned in the download1 parameter and unzip in the folder
   ~/.jdosbox/<zip name>/ then it is up to you to issue the proper mount command.  You can either
   mount an image file you just downloaded via imgmount.  Or you can mount the entire directory.

You have to specifiy the width and height of the applet object ahead of time, but jdosbox will set the
size of the screen it will use at run time.  If the jdosbox screen is smaller than the applet window
then the screen will be centered.  The background color around the screen defaults to dark grey, but you
may change that by setting the param "background-color".

   Example: <param name="background-color" value="#FFFFFF">


===================
3. NE2000 Ethernet:
===================

This requires native code which is why it is not part of the standard build

See the ReadMe.txt in the pcap directory.