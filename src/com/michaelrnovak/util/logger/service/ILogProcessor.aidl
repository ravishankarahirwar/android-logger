package com.michaelrnovak.util.logger.service;

interface ILogProcessor {
	
	void reset();
	void run();
	void stop();
	void write(String file);
}
