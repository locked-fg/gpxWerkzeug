# todo

# run

CLI Command:
--gpxSrc="C://foo/gpx1/" --gpxSrc="C://bar/gpx2"

## interactive shell

- build jar
- ```jshell --class-path target\backend-0.0.1-SNAPSHOT.jar```
- ```import de.locked.GpxWerkzeug.gpx.*; ```
- ```

File getTestFile(String filename) { return new File(Objects.requireNonNull(
Gpx.class.getClassLoader().getResource(filename)).getFile()
);
}
```
