package de.fjak.spoofax.batchtest;

import static java.lang.System.currentTimeMillis;

class Stopwatch {
	private long    elapsed   = 0L;
	private long    start     = currentTimeMillis();
	private boolean isRunning = false;

	private Stopwatch update() {
		elapsed += currentTimeMillis() - start;
		start = currentTimeMillis();
		return this;
	}

	public Stopwatch start() {
		if (!isRunning) {
			start = currentTimeMillis();
			isRunning = true;
		}
		return this;
	}

	public Stopwatch stop() {
		if (isRunning) {
			update();
			isRunning = false;
		}
		return this;
	}

	public Stopwatch reset() {
		elapsed = 0L;
		start = currentTimeMillis();
		isRunning = false;
		return this;
	}

	public long read() {
		if (isRunning) {
			update();
		}
		return elapsed;
	}
}