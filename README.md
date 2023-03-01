# Satisplanory

A desktop application with a production calculator for Satisfactory.

## Download

You can get the latest version here: https://github.com/elcheapogary/satisplanory/releases/latest

## Installation

1. Download the zip file
2. Extract the zip file. Like right-click, `Extract all`. There is no special place you should extract the 
   zip to - anywhere is fine. 
3. Run `satisplanory.exe` in the extracted zip folder. Don't run the EXE from inside the zip file - it will
   not work. Make sure you first extract the zip, then run the EXE from inside the extracted folder.

Satisplanory uses recipe and item data that is shipped with Satisfactory. To read this data, Satisplanory
needs to locate your installation of Satisfactory on your PC. Don't worry, Satisplanory makes no changes to
the Satisfactory installation, it just read some data to use in calculations.

The first time you run Satisplanory, it will try and figure out where you have Satisfactory installed. If
it cannot find Satisfactory, you will be prompted to select your Satisfactory installation directory. Just
browse to where ever your Satisfactory installation is and select it. On Steam the folder should be called
`Satisfactory`, on Epic, it should be `SatisfactoryEarlyAccess` or `SatisfactoryExperimental`.

## Discord

Join our Discord at https://discord.gg/NncUggkwQB to be notified of new version, provide feedback, request features,
or to ask for help.

## Alternatives

Don't like this calculator? Try these:

- https://www.satisfactory-planner.net/
- https://www.satisfactorytools.com/
- https://satisfactory-calculator.com/
- https://daniel2013.github.io/satisfactory/

## Building from source

Only developers would need to build Satisplanory from source - normal users should just use the
download link above.

To build Satisplanory from source, you'll need Apache Maven and OpenJDK 17.

To build the Windows distribution zip file, run the following command on a Windows machine: 

```
mvn -P "jlink,windows" clean verify
```

To build the Linux distribution tar file, run the following command on a Linux machine:

```
mvn -P "jlink,linux" clean verify
```

The distribution files will be in the `target/dist/` directory.

To launch the app from the source code, without building a distribution image, do: 

```
mvn javafx:run
```