# Wishbone Bus Generator

Generate a WISHBONE bus interconnect for a list of master(s) and slave(s) interfaces

## Build and run

From the project folder: 

First pull git submodules:

```
git submodule update --init --recursive
```

Then build the project:

```
mkdir build
cd build
cmake ..
cmake --build .
```

And run with:

```
wihbone-gen
```

(No args, only work with an example for now)