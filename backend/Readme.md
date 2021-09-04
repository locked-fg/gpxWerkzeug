# Yet another GPX Toolbox

## Aims of the project:
- easy handling of GPX data from my 'Garmin Dakota 20'
- learn :-)
- to be extended

## Links
- an elaborate GPX library: https://github.com/jenetics/jpx
- GPX XML Schema: http://www.topografix.com/GPX/1/1/

## interactive shell
- build jar
- ```jshell --class-path target\backend-0.0.1-SNAPSHOT.jar```
- ```import de.locked.GpxWerkzeug.gpx.*; ```
- ```
File getTestFile(String filename) {
    return new File(Objects.requireNonNull(
            Gpx.class.getClassLoader().getResource(filename)).getFile()
    );
}

```
