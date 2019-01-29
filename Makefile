SRC_DIR = src
OUT_DIR = exe
LIB_DIR = samiam
CP = ${LIB_DIR}:${LIB_DIR}/inflib.jar:include/*
OPTS = -Xlint:unchecked
SRC_FILES = $(shell find ${SRC_DIR} -name "*.java")
JAVA = javac
CC = g++ -std=c++0x
CFLAGS = -Wno-sign-compare
all:
	${JAVA} -d ${OUT_DIR} -sourcepath ${SRC_DIR} -cp ${CP} ${OPTS} ${MAIN} ${SRC_FILES}
	$(CC) $(CFLAGS) -O2 -Wall -I./include   -c -o src/oddtosdd.o src/oddtosdd.cpp
	$(CC) $(CFLAGS) -O2 -Wall -I./include src/oddtosdd.o -L./lib -lsdd -lm -o exe/oddtosdd

clean:
	@rm -rf ${OUT_DIR}/*
