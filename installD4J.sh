cd D4J

if [[ -d defects4j ]] ; then
    rm -rf defects4j 
fi

git clone https://github.com/rjust/defects4j.git
cd defects4j
./init.sh

cd ..
cd ..
