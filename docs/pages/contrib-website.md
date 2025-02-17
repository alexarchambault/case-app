# Website

## Building the website locally

### Watch mode

Run
```text
$ ./mill -i docs.mkdocsServe
```

Then open the URL printed in the console in your browser (it should be
[`http://127.0.0.1:8000`](http://127.0.0.1:8000))

### Once

Build the website in the `docs/site` directory with
```text
$ ./mill -i docs.mkdocsBuild
```
