# First delete any kind of previous build directory
if [ -d build ]; then
    rm -r build
fi

# Figure out which classpath separator to use (windows is ';', linux is ':')
case "$(uname -s)" in
    Darwin)
        sep=':'
        ;;

    Linux)
        sep=':'
        ;;

    *)
        sep=';'
        ;;
esac

# Compile
javac -d build -cp "lib/*${sep}compilers/*${sep}compilers/util/*" compilers/*.java compilers/util/*.java

# Package
pushd build
# Make the manifest to make the jar executable
cat <<- EOF > MANIFEST.MF
Manifest-Version: 1.0
Main-Class: compilers.CFGReader
Class-Path: lib/commons-collections4-4.4.jar
EOF

jar cvfm ../CFGReader.jar MANIFEST.MF compilers/* ../lib/*
popd

# Clean-up?
rm -r build
