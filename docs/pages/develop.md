# Contributing

## Building the website locally

### Watch mode

In a first terminal, run
```text
$ ./mill -i -w docs.mdocWatch
```

Leave it running, and run in a second terminal
```text
$ ./mill -i -w docs.mkdocsServe
```

Then open the URL printed in the console in your browser (it should be
[`http://127.0.0.1:8000`](http://127.0.0.1:8000))

### Once

Build the website in the `docs/site` directory with
```text
$ ./mill -i docs.mkdocsBuild
```
