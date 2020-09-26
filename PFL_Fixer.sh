
proj=Time
for id in $(seq 1 27)
do
    java -Xmx4g -cp "target/dependency/*" edu.lu.uni.serval.tbar.main.MainPerfectFL ${proj}_${id}
done

