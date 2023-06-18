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
mvn -P "jlink,windows" clean install
```

To build the Linux distribution tar file, run the following command on a Linux machine:

```
mvn -P "jlink,linux" clean install
```

The distribution files will be in the `satisplanory-app/target/dist/` directory.

To launch the app from the source code, without building a distribution image, do: 

```
mvn clean install
cd satisplanory-app
mvn javafx:run
```