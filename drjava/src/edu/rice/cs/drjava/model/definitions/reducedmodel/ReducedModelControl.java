/*BEGIN_COPYRIGHT_BLOCK
 *
 * Copyright (c) 2001-2008, JavaPLT group at Rice University (drjava@rice.edu)
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

package edu.rice.cs.drjava.model.definitions.reducedmodel;

import java.util.Vector;

import edu.rice.cs.util.UnexpectedException;

/** This class provides an implementation of the BraceReduction interface for brace matching.  In order to correctly
  * match, this class keeps track of what is commented (line and block) and what is inside double quotes (strings).
  * To avoid unnecessary complication, this class maintains a few invariants for its  consistent states, i.e., between
  * top-level function calls.
  * <ol>
  * <li> The cursor offset is never at the end of a brace.  If movement or insertion puts it there, the cursor is 
  * updated to point to the 0 offset of the next brace.
  * <li> Quoting information is invalid inside valid comments.  When part of the document becomes uncommented, the
  * reduced model must update the quoting information linearly in the newly revealed code.
  * <li> Quote shadowing and comment shadowing are mutually exclusive.
  * <li> There is no nesting of comment open characters. If // is encountered in the middle of a comment, it is 
  * treated as two separate slashes.  Similarly for /*.
  * </ol>
  * All of the code in the class assumes that a lock on this is held.
  * @author JavaPLT
  * @version $Id$
  */
public class ReducedModelControl implements BraceReduction {
  /* private fields; default visibility for testing purposes only. */
  final ReducedModelBrace _rmb;   // the reduced brace model
  final ReducedModelComment _rmc; // the reduced comment model
  volatile int _offset;

  /** Standard constructor. */
  public ReducedModelControl() {
    _rmb = new ReducedModelBrace(this);
    _rmc = new ReducedModelComment();
  }
  
//  private ReducedModelBrace _getRMB() { return _rmb; }
  
//  private ReducedModelComment _getRMC() { return _rmc; }
  
  public void insertChar(char ch) {
    _rmb.insertChar(ch);
    _rmc.insertChar(ch);
  }

  /** Updates the BraceReduction to reflect cursor movement. Negative values move left; positive values move right.
    * @param count indicates the direction and magnitude of cursor movement
    */
  public void move(int count) {
    try {
      _rmb.move(count);
      _rmc.move(count);
    }
    catch(IllegalArgumentException e) { 
      resetLocation();
      throw new UnexpectedException(e);
    }
  }

  /** Updates the BraceReduction to reflect text deletion.
    * @param count indicates the size and direction of text deletion. Negative values delete text to the left of the
    *  cursor, positive values delete text to the right.
    */
  public void delete(int count) {
    _rmb.delete(count);
    _rmc.delete(count);
  }

  /** Finds the closing brace that matches the next significant brace iff that brace is an open brace.</P>
    * @return the distance until the matching closing brace.  On failure, returns -1.
    * @see #balanceBackward()
    */
  public int balanceForward() { return _rmb.balanceForward(); }
  
  /** Finds the open brace that matches the previous significant brace iff that brace is an closing brace.</P>
    * @return the distance until the matching open brace.  On failure, returns -1.
    * @see #balanceForward()
    */
  public int balanceBackward() { return _rmb.balanceBackward(); }

  /** Returns the state at the relDistance, where relDistance is relative to the last time it was called. You can reset
    * the last call to the current offset using resetLocation.
    */
  public ReducedModelState moveWalkerGetState(int relDistance) { return _rmc.moveWalkerGetState(relDistance); }

  /** This function resets the location of the walker in the comment list to where the current cursor is. This allows
    * the walker to keep walking and using relative distance instead of having to rewalk the same distance every call
    * to stateAtRelLocation. It is an optimization.
    */
  public void resetLocation() {
    _rmc.resetWalkerLocationToCursor();
  }

  /** Gets the token currently pointed at by the cursor. Because the reduced model is split into two reduced sub-models,
    * we have to check each sub-model first as each one has unique information.  If we find a non-gap token in either 
    * sub-model we want to return that.  Otherwise, we want to return a sort of hybrid Gap of the two, i.e., a Gap where
    * there are neither special comment/quote tokens nor parens/squigglies/brackets.
    * @return a ReducedToken representative of the unified reduced model
    */
  public ReducedToken currentToken() {
    // check the reduced comment model for specials
    ReducedToken rmcToken = _rmc.current();
    if (! rmcToken.isGap()) return rmcToken;
    // check the reduced brace model for braces
    ReducedToken rmbToken = _rmb.current();
    if (! rmbToken.isGap()) {
      rmbToken.setState(_rmc.getStateAtCurrent());
      return rmbToken;
    }
    // otherwise, we have a gap.
    int size = getSize(rmbToken,rmcToken);
    return new Gap(size, _rmc.getStateAtCurrent());
  }

  /** Gets the shadowing state at the current caret position.
    * @return FREE|INSIDE_LINE_COMMENT|INSIDE_BLOCK_COMMENT|
    * INSIDE_SINGLE_QUOTE|INSIDE_DOUBLE_QUOTE
    */
  public ReducedModelState getStateAtCurrent() { return _rmc.getStateAtCurrent(); }
  
  /** Get a string representation of the current token's type.
    * @return "" if current is a Gap, otherwise, use ReducedToken.getType()
    */
  String getType() {
    ReducedToken rmcToken = _rmc.current();
    if (! rmcToken.isGap())
      return rmcToken.getType();

    ReducedToken rmbToken = _rmb.current();
    if (! rmbToken.isGap()) {
      return rmbToken.getType();
    }
    return ""; //a gap
  }

  /** Gets the size of the current token. It checks both the brace and comment sub-models to find the size of the
    * current token.  If the current token is a Gap, we have to reconcile the information of both sub-models in order
    * to get the correct size of the current token as seen by the outside world.
    * @return the number of characters represented by the current token
    */
  int getSize() {
    return getSize(_rmb.current(),_rmc.current());
  }

  int getSize(ReducedToken rmbToken, ReducedToken rmcToken) {
    int rmb_offset = _rmb.getBlockOffset();
    int rmc_offset = _rmc.getBlockOffset();
    int rmb_size = rmbToken.getSize();
    int rmc_size = rmcToken.getSize();
    int size;
    if (rmb_offset < rmc_offset) {
      size = rmb_offset;
      _offset = size;
    }
    else {
      size = rmc_offset;
      _offset = size;
    }

    if (rmb_size - rmb_offset < rmc_size - rmc_offset) {
      size += (rmb_size - rmb_offset);
    }
    else {
      size += (rmc_size - rmc_offset);
    }
    return size;
  }

  /** Move the reduced model to the next token and update the cursor information. */
  void next() {
    if (_rmc._cursor.atStart()) {
      _rmc.next();
      _rmb.next();
      return;
    }
    int size = getSize(_rmb.current(), _rmc.current());
    _rmc.move(size - _offset);
    _rmb.move(size - _offset);
  }

  /** Move the reduced model to the previous token and update the cursor information. */
  void prev() {
    int size;
    if (_rmc._cursor.atEnd()) {
      _rmc.prev();
      _rmb.prev();
      if (_rmc._cursor.atStart()) {
        return; // because in place now.
      }

      if (_rmc.current().getSize() < _rmb.current().getSize()) {
        size = -_rmc.current().getSize();
      }
      else {
        size = -_rmb.current().getSize();
      }
      _rmc.next();
      _rmb.next();
      move(size);
    }
    else if (_rmb.getBlockOffset() < _rmc.getBlockOffset()) {
      _rmb.prev();
      size = _rmb.current().getSize() + _rmb.getBlockOffset();
      _rmb.next();
      if (size < _rmc.getBlockOffset()) {
        move(-size);
      }
      else {
        move(-_rmc.getBlockOffset());
      }
    }
    else if (_rmb.getBlockOffset() == _rmc.getBlockOffset()) {
      _rmb.prev();
      _rmc.prev();
      _rmb.setBlockOffset(0);
      _rmc.setBlockOffset(0);
    }
    else {
      _rmc.prev();
      size = _rmc.current().getSize() + _rmc.getBlockOffset();
      _rmc.next();
      if (size < _rmb.getBlockOffset()) {
        move(-size);
      }
      else {
        move(-_rmb.getBlockOffset());
      }
    }
  }

  /** Get the previous token. */
  public ReducedToken prevItem() {
    int rmbOffset = _rmb.getBlockOffset();
    int rmcOffset = _rmc.getBlockOffset();

    prev();
    ReducedToken temp = currentToken();
    next();

    _rmb.setBlockOffset(rmbOffset);
    _rmc.setBlockOffset(rmcOffset);
    return temp;
  }

  /** Get the next token. */
  public ReducedToken nextItem() {
    int rmbOffset = _rmb.getBlockOffset();
    int rmcOffset = _rmc.getBlockOffset();
    next();
    ReducedToken temp = currentToken();
    prev();
    _rmb.setBlockOffset(rmbOffset);
    _rmc.setBlockOffset(rmcOffset);
    return temp;
  }

  /** Determines if the cursor is at the end of the reduced model. */
  boolean atEnd() { return (_rmb._cursor.atEnd() || _rmc._cursor.atEnd()); }

  /** Determines if the cursor is at the start of the reduced model. */
  boolean atStart() { return (_rmb._cursor.atStart() || _rmc._cursor.atStart()); }

  /** Gets the offset within the current token. */
  int getBlockOffset() {
    if (_rmb.getBlockOffset() < _rmc.getBlockOffset()) return _rmb.getBlockOffset();
    return _rmc.getBlockOffset();
  }

  /** Gets the absolute character offset into the document represented by the reduced model. */
  public int absOffset() { return _rmc.absOffset(); }


  /** A toString() substitute. */
  public String simpleString() {
    return "\n********\n" + _rmb.simpleString() + "\n________\n" + _rmc.simpleString();
  }

  /** Returns an IndentInfo containing the following information:
    * - distance to the previous newline (start of this line)
    * - distance to the brace enclosing the beginning of the current line
    * - distance to the beginning of the line containing that brace
    */
  public IndentInfo getIndentInformation() {
    IndentInfo braceInfo = new IndentInfo();
    //get distance to the previous newline (in braceInfo.distToNewline)
    _rmc.getDistToPreviousNewline(braceInfo);
    //get distance to the closing brace before that new line.
    _rmb.getDistToEnclosingBrace(braceInfo);
    //get distance to newline before the previous, just mentioned, brace.
    _rmc.getDistToIndentNewline(braceInfo);
    // get distance to the brace enclosing the current location
    _rmb.getDistToEnclosingBraceCurrent(braceInfo);
    // get distance to the beginning of that brace's line
    _rmc.getDistToCurrentBraceNewline(braceInfo);
    return braceInfo;
  }
  
  
  

  public int getDistToIdentNewline() { return -1; }
  public int getDistToCurrentBraceNewline() { return -1; }
  
  /** Gets info about the brace enclosing the beginning of this line. */
  public BraceInfo getEnclosingBrace() { return _rmb.getEnclosingBrace(); }
  /** Gets info about the brace enclosing this location. */
  public BraceInfo getEnclosingBraceCurrent() { return _rmb.getEnclosingBraceCurrent(); }
  /** Gets distance to the new newline character (not including the newline). */
  public int getDistToPreviousNewline() { return _rmc.getDistToPreviousNewline(); }
  /** Gets distance to previous newline character (not including the newline). */
  public int getDistToPreviousNewline(int relLoc) { return _rmc.getDistToPreviousNewline(relLoc); }

  public int getDistToNextNewline() { return _rmc.getDistToNextNewline(); }

  /** Return all highlight status info for text between the current location and current location + length.  This should
    * collapse adjoining blocks with the same status into one.
    * @param start The start location of the area for which we want the status.  The reduced model is already at this
    *    position, but this value is needed to determine the absolute positions in HighlightStatus objects we return.
    * @param length The length of the text segment for which status information must be generated.
    */
  public Vector<HighlightStatus> getHighlightStatus(final int start, final int length) {
    Vector<HighlightStatus> vec = new Vector<HighlightStatus>();

    int curState;
    int curLocation;
    int curLength;

    TokenList.Iterator cursor = _rmc._cursor._copy();
//    int ct = rmc._tokens.listenerCount();
    curLocation = start;
    // NOTE: old code threw an exception if cursor.atStart(); it used wrong value for curLength atEnd too
//    curLength = ! cursor.atEnd() ? cursor.current().getSize() - rmc.getBlockOffset() : start + length; 
//    curState = ! cursor.atEnd() ? cursor.current().getHighlightState() : 0;
    if (cursor.atEnd() || cursor.atStart()) { // cursor is not inside a reduced model token
      curLength = length;
      curState = 0;
    }
    else {
      curLength = cursor.current().getSize() - _rmc.getBlockOffset();
      curState = cursor.current().getHighlightState();
    }

    while ((curLocation + curLength) < (start + length)) {
      cursor.next();
      //TODO: figure out why this function is iterating past the end of the collection
      //when it gets called from the ColoringGlyphPainter after deleting the last character
      if (cursor.atEnd()) break;
      int nextState = cursor.current().getHighlightState();

      if (nextState == curState) {
        // add on and keep building
        curLength += cursor.current().getSize();
      }
      else {
        // add old one to the vector and start new one
        vec.add(new HighlightStatus(curLocation, curLength, curState));
        curLocation += curLength; // new block starts after previous one
        curLength = cursor.current().getSize();
        curState = nextState;
      }
    }

    // Make sure this token length doesn't extend past start+length.
    // This is because we guarantee that the returned vector only refers
    // to chars on [start, start+length).
    int requestEnd = start + length;
    if ((curLocation + curLength) > requestEnd) {
      curLength = requestEnd - curLocation;
    }

    // Add the last one, which has not been added yet
    vec.add(new HighlightStatus(curLocation, curLength, curState));

    cursor.dispose();

    return vec;
  }
}
