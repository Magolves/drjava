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

package edu.rice.cs.javalanglevels;
import java.util.*;

/** This class extends Hashtable so that we can have extra functionality in the put function.
  * A Symboltable is specifically a Hashtable of Strings to SymbolData.  The put function
  * checks to see if the specified SymbolData is already in the table.  If so, it simply updates its
  * fields.
  */

public class Symboltable extends Hashtable<String, SymbolData> {
  
  /** Overrides put function of hash table.  If the specified SymbolData is already in
    * the table, simply update its fields.  Otherwise, do a normal put operation.
    */
  public SymbolData put (String name, SymbolData sd) {
    SymbolData inTable = this.get(sd.getName());
    if (inTable != null) {
      /**Replace all its fields with those of sd.*/
      inTable.setIsContinuation(sd.isContinuation());
      inTable.setTypeParameters(sd.getTypeParameters());
      inTable.setMethods(sd.getMethods());
      inTable.setSuperClass(sd.getSuperClass());
      inTable.setInterfaces(sd.getInterfaces());
      inTable.setOuterData(sd.getOuterData());
      inTable.setInnerClasses(sd.getInnerClasses());
    }
    else {
      super.put(sd.getName(), sd);
    }
    
    return sd;
  }
  
  public SymbolData get (String name) {
    return super.get(name);
  }
}