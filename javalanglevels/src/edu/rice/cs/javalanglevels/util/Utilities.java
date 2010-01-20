/*BEGIN_COPYRIGHT_BLOCK
 *
 * Copyright (c) 2001-2010, JavaPLT group at Rice University (drjava@rice.edu)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the names of DrJava, the JavaPLT group, Rice University, nor the
 *      names of its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software is Open Source Initiative approved Open Source Software.
 * Open Source Initative Approved is a trademark of the Open Source Initiative.
 * 
 * This file is part of DrJava.  Download the current version of this project
 * from http://www.drjava.org/ or http://sourceforge.net/projects/drjava/
 * 
 * END_COPYRIGHT_BLOCK*/

package edu.rice.cs.javalanglevels.util;

import java.awt.EventQueue;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.datatransfer.*;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.io.IOException;

public class Utilities {
  
  /** Runs the task synchronously if the current thread is the event thread; otherwise passes it to the
    * event thread to be run asynchronously after all events already on the queue have been processed.
    */
  public static void invokeLater(Runnable task) {
    if (EventQueue.isDispatchThread()) {
      task.run(); 
      return;
    }
    EventQueue.invokeLater(task);
  }
  
  public static void invokeAndWait(Runnable task) {
    if (EventQueue.isDispatchThread()) {
      task.run(); 
      return;
    }
    try { EventQueue.invokeAndWait(task); }
    catch(Exception e) { throw new RuntimeException(e); }
  }
  
  public static void main(String[] args) { clearEventQueue(); }
  
  public static void clearEventQueue() {
    Utilities.invokeAndWait(new Runnable() { public void run() { } });
  }
  
  /** Show a modal debug message box with an OK button regardless of TEST_MODE.
    * @param msg string to display
    */
  public static void show(final String msg) { 
    Utilities.invokeAndWait(new Runnable() { public void run() {
      new ScrollableDialog(null,"Debug Message", "Debug Message from Utilities.show():", msg, false).show(); } } );
  }
  
  /** Shows a modal debug message box with an OK button when not in TEST_MODE.
    * @param msg string to display
    */
  public static void showDebug(String msg) { showMessageBox(msg, "Debug Message"); }
  
  /** Shows a modal message box with an OK button.
    * @param msg string to display
    */
  public static void showMessageBox(final String msg, final String title) {
    // Utilities.invokeAndWait(new Runnable() { public void run() { JOptionPane.showMessageDialog(null, msg); } } );
    Utilities.invokeAndWait(new Runnable() { public void run() {
      new ScrollableDialog(null, title, "Message:", msg, false).show();
    } } );
  }
  
  public static void showStackTrace(final Throwable t) {
    Utilities.invokeAndWait(new Runnable() { public void run() { 
      new ScrollableDialog(null, "Stack Trace", "Stack Trace:", getStackTrace(t), false).show();
    } } );
  }
  
  /** @return a string with the current clipboard selection, or null if not available. */
  public static String getClipboardSelection(Component c) {
    Clipboard cb = c.getToolkit().getSystemClipboard();
    if (cb == null) return null;
    Transferable t = cb.getContents(null);
    if (t == null) return null;
    String s = null;
    try {
      java.io.Reader r = DataFlavor.stringFlavor.getReaderForText(t);
      int ch;
      final StringBuilder sb = new StringBuilder();
      while ((ch=r.read()) !=-1 ) { sb.append((char)ch); }
      s = sb.toString();
    }
    catch(UnsupportedFlavorException ufe) { /* ignore, return null */ }
    catch(java.io.IOException ioe) { /* ignore, return null */ }
    return s;
  }
  
  /** @return an action with a new name that delegates to another action. */
  public static AbstractAction createDelegateAction(String newName, final Action delegate) {
    return new AbstractAction(newName) {
      public void actionPerformed(ActionEvent ae) { delegate.actionPerformed(ae); }
    };
  }
    /** Gets the stack trace of the given Throwable as a String.
    * @param t the throwable object for which to get the stack trace
    * @return the stack trace of the given Throwable
    */
  public static String getStackTrace(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    return sw.toString();
  }
  
  /** Gets the stack trace of the current code. Does not include this method.
    * @return the stack trace for the current code
    */
  public static String getStackTrace() {
    try { throw new Exception(); } // Thread.getStackTrace() might be more efficient, but is new in Java 5.0
    catch (Exception e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      StackTraceElement[] stes = e.getStackTrace();
      int skip = 1;
      for(StackTraceElement ste: stes) {
        if (skip>0) { --skip; } else { pw.print("at "); pw.println(ste); }
      }
      return sw.toString();
    }
  }
}
