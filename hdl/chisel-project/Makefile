

all:
	sbt "runMain Main"
	cp -r xilinx/wrapper/* build/

run:
	sbt run
	cp -r xilinx/wrapper/*.v build/

clean:
	rm -rf build/