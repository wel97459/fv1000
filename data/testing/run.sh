#!/bin/bash
asl -cpu z80 -P -x -i ./ -L $1.asm
p2bin $1.p $1.bin
#mv out.i out.asm
