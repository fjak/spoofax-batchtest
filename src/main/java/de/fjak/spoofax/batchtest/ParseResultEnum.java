package de.fjak.spoofax.batchtest;

public enum ParseResultEnum {
	AMBIGUITY('A', "Ambiguity"),
	ERROR('E', "Error"),
	FAILURE('F', "Failure"),
	STARTMISS('S', "Startsymbol Mismatch"),
	SUCCESS('.', "Success"),
	TIMEOUT('T', "Timeout");

	char   signal;
	String name;

	ParseResultEnum(char signal, String name) {
		this.signal = signal;
		this.name = name;
	}
}