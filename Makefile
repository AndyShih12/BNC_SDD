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

clean:
	@rm -rf ${OUT_DIR}/*
