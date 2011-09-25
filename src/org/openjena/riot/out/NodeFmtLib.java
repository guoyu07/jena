/*
 * (c) Copyright 2009 Talis Systems Ltd.
 * (c) Copyright 2010, 2011 Epimorphics Ltd.
 * All rights reserved.
 * [See end of file]
 */

package org.openjena.riot.out;

import java.io.StringWriter ;
import java.io.Writer ;
import java.net.MalformedURLException ;
import java.util.Map ;

import org.openjena.atlas.lib.Bytes ;
import org.openjena.atlas.lib.Chars ;
import org.openjena.riot.system.PrefixMap ;
import org.openjena.riot.system.Prologue ;
import org.openjena.riot.system.RiotChars ;

import com.hp.hpl.jena.graph.Node ;
import com.hp.hpl.jena.graph.Triple ;
import com.hp.hpl.jena.iri.IRI ;
import com.hp.hpl.jena.iri.IRIFactory ;
import com.hp.hpl.jena.iri.IRIRelativize ;
import com.hp.hpl.jena.shared.PrefixMapping ;
import com.hp.hpl.jena.sparql.ARQConstants ;
import com.hp.hpl.jena.sparql.core.Quad ;

/** Presentation utilitiles for Nodes, Triples, Quads and more */ 
public class NodeFmtLib
{
    // See OutputLangUtils.
    // See and use EscapeStr
    
    static PrefixMap dftPrefixMap = new PrefixMap() ;
    static {
        PrefixMapping pm = ARQConstants.getGlobalPrefixMap() ;
        Map<String, String> map = pm.getNsPrefixMap() ;
        for ( Map.Entry<String, String> e : map.entrySet() )
            dftPrefixMap.add(e.getKey(), e.getValue() ) ;
    }

    public static String str(Triple t)
    {
        return str(t.getSubject(), t.getPredicate(),t.getObject()) ;
    }

    public static String str(Quad q)
    {
        return str(q.getGraph(), q.getSubject(), q.getPredicate(), q.getObject()) ;
    }
    

    // Worker
    public static String str(Node ... nodes)
    {
        StringWriter sw = new StringWriter() ;
        boolean first = true ;
        for ( Node n : nodes ) 
        {
            if ( ! first )
            {
                sw.append(" ") ;
                first = false ;
            }
            str(sw, n) ;
        }
        return sw.toString() ; 
    }

    //public static String str(Node n)

    private static final boolean onlySafeBNodeLabels = true ;

    //public static String displayStr(Node n) { return serialize(n) ; }

    public static void str(Writer w, Node n)
    { serialize(w, n, null, null) ; }

    public static void serialize(Writer w, Node n, Prologue prologue)
    { serialize(w, n, prologue.getBaseURI(), prologue.getPrefixMap()) ; }

    
    public static void serialize(Writer w, Node n, String base, PrefixMap prefixMap)
    {
        if ( prefixMap == null )
            prefixMap = dftPrefixMap ;
        NodeFormatter formatter = new NodeFormatterTTL(base, prefixMap) ;
        formatter.format(w, n) ;
    }
    
    // ---- Blank node labels.
    
    // Strict N-triples only allows [A-Za-z][A-Za-z0-9]
    static char encodeMarkerChar = 'X' ;

    // These two form a pair to convert bNode labels to a safe (i.e. legal N-triples form) and back agains. 
    
    // Encoding is:
    // 1 - Add a Letter 
    // 2 - Hexify, as Xnn, anything outside ASCII A-Za-z0-9
    // 3 - X is encoded as XX
    
    private static char LabelLeadingLetter = 'B' ; 
    
    public static String encodeBNodeLabel(String label)
    {
        StringBuilder buff = new StringBuilder() ;
        // Must be at least one char and not a digit.
        buff.append(LabelLeadingLetter) ;
        
        for ( int i = 0 ; i < label.length() ; i++ )
        {
            char ch = label.charAt(i) ;
            if ( ch == encodeMarkerChar )
            {
                buff.append(ch) ;
                buff.append(ch) ;
            }
            else if ( RiotChars.isA2ZN(ch) )
                buff.append(ch) ;
            else
                Chars.encodeAsHex(buff, encodeMarkerChar, ch) ;
        }
        return buff.toString() ;
    }

    // Assumes that blank nodes only have characters in the range of 0-255
    public static String decodeBNodeLabel(String label)
    {
        StringBuilder buffer = new StringBuilder() ;

        if ( label.charAt(0) != LabelLeadingLetter )
            return label ;
        
        // Skip first.
        for ( int i = 1; i < label.length(); i++ )
        {
            char ch = label.charAt(i) ;
            
            if ( ch != encodeMarkerChar )
            {
                buffer.append(ch) ;
            }
            else
            {
                // Maybe XX or Xnn.
                char ch2 = label.charAt(i+1) ;
                if ( ch2 == encodeMarkerChar )
                {
                    i++ ;
                    buffer.append(ch) ;
                    continue ;
                }
                
                // Xnn
                i++ ;
                char hiC = label.charAt(i) ;
                int hi = Bytes.hexCharToInt(hiC) ;
                i++ ;
                char loC = label.charAt(i) ;
                int lo = Bytes.hexCharToInt(loC) ;

                int combined = ((hi << 4) | lo) ;
                buffer.append((char) combined) ;
            }
        }

        return buffer.toString() ;
    }
    
    // ---- Relative URIs.
    
    static private int relFlags = IRIRelativize.SAMEDOCUMENT | IRIRelativize.CHILD ;
    static public String abbrevByBase(String uri, String base)
    {
        if ( base == null )
            return null ;
        IRI baseIRI = IRIFactory.jenaImplementation().construct(base) ;
        IRI rel = baseIRI.relativize(uri, relFlags) ;
        String r = null ;
        try { r = rel.toASCIIString() ; }
        catch (MalformedURLException  ex) { r = rel.toString() ; }
        return r ;
    }

    // ---- Escaping.
    
    static boolean applyUnicodeEscapes = false ;
    
    static EscapeStr escaper = new EscapeStr(false) ; 
    
    // take a string and make it safe for writing.
    public static String stringEsc(String s)
    { 
        return stringEsc(s, true) ;
    }
    
    public static String stringEsc(String s, boolean singleLineString)
    {
        StringWriter sw = new StringWriter() ;
        if ( singleLineString )
            escaper.writeStr(sw, s) ;
        else
            escaper.writeStrMultiLine(sw, s) ;
        return sw.toString() ;
    }
    
    public static void stringEsc(StringBuilder sbuff, String s)
    { stringEsc( sbuff,  s, true ) ; }

    public static void stringEsc(StringBuilder sbuff, String s, boolean singleLineString)
    {
        int len = s.length() ;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);

            // Escape escapes and quotes
            if (c == '\\' || c == '"' )
            {
                sbuff.append('\\') ;
                sbuff.append(c) ;
                continue ;
            }
            
            // Characters to literally output.
            // This would generate 7-bit safe files 
//            if (c >= 32 && c < 127)
//            {
//                sbuff.append(c) ;
//                continue;
//            }    

            // Whitespace
            if ( singleLineString && ( c == '\n' || c == '\r' || c == '\f' ) )
            {
                if (c == '\n') sbuff.append("\\n");
                if (c == '\t') sbuff.append("\\t");
                if (c == '\r') sbuff.append("\\r");
                if (c == '\f') sbuff.append("\\f");
                continue ;
            }
            
            // Output as is (subject to UTF-8 encoding on output that is)
            
            if ( ! applyUnicodeEscapes )
                sbuff.append(c) ;
            else
            {
                // Unicode escapes
                // c < 32, c >= 127, not whitespace or other specials
                if ( c >= 32 && c < 127 )
                {
                    sbuff.append(c) ;
                }
                else
                {
                    String hexstr = Integer.toHexString(c).toUpperCase();
                    int pad = 4 - hexstr.length();
                    sbuff.append("\\u");
                    for (; pad > 0; pad--)
                        sbuff.append("0");
                    sbuff.append(hexstr);
                }
            }
        }
    }
    
//    public static String stringEsc(String s)    { return FmtUtils.stringEsc(s) ; }
//    
//    public static String stringEsc(String s, boolean singleLineString)
//    { return FmtUtils.stringEsc(s, singleLineString) ; }
//
//    public static String unescapeStr(String s)    { return ParserBase.unescapeStr(s) ; }
    
}

/*
 * (c) Copyright 2009 Talis Systems Ltd.
 * (c) Copyright 2010, 2011 Epimorphics Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */