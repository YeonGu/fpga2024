all:
	dtc -I dts -O dtb -o user-override.dtb user-modified.dts

origin:
	dtc -I dts -O dtb -o user-override.dtb original-hack.dts

reverse:
	dtc -I dtb -O dts -o modified-hack.dts user-override.dtb