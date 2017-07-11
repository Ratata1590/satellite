package com.ratata.dynamicCodeRest.dynamicObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import com.ratata.dynamicCodeRest.utils.ErrorUtil;

public class ProcessThread extends Thread {

  private final static int bufferSize = 10000;

  private Process process;
  private List<String> command;
  private String internalError = "";
  private boolean ended = false;

  private BufferedWriter input;
  private BufferedReader output;
  private BufferedReader error;

  public void run() {
    try {
      ProcessBuilder pb = new ProcessBuilder(command);
      process = pb.start();
      input = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
      output = new BufferedReader(new InputStreamReader(process.getInputStream()));
      error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      process.waitFor();
    } catch (Exception e) {
      internalError = ErrorUtil.printStackTrace(e);
    }
    ended = true;
  }

  public ProcessThread(List<String> command) throws Exception {
    this.command = command;
    this.start();
  }

  public void sendInput(String inputData) throws Exception {
    input.write(inputData);
    input.flush();
  }

  public String getOutput() throws Exception {
    if (!output.ready()) {
      throw new Exception("not ready");
    }
    char[] buffer = new char[bufferSize];
    int charCount = output.read(buffer);
    return new String(buffer, 0, charCount);
  }

  public String getError() throws Exception {
    if (!error.ready()) {
      throw new Exception("not ready");
    }
    char[] buffer = new char[bufferSize];
    int charCount = error.read(buffer);
    return new String(buffer, 0, charCount);
  }

  public String getInternalError() {
    return internalError;
  }

  public boolean getEnded() {
    return ended;
  }

  public List<String> getCommand() {
    return command;
  }

  public void closeProcess() {
    try {
      process.destroy();
      if (process.isAlive()) {
        process.destroyForcibly();
      }
    } catch (Exception e) {

    }
    try {
      input.close();
    } catch (Exception e) {

    }
    try {
      output.close();
    } catch (Exception e) {

    }
    try {
      error.close();
    } catch (Exception e) {

    }
  }
}
