/**
 * Copyright (c) 2005, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.awt.Color;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.ChatBuffer;

public class LimitedSizeChatBuffer extends ChatBuffer
{
	private List highlights;
	private List dehighlights;
	private LimitedSizeChatBuffer highlightBuffer;

	private static final int BUFFER_LIMIT = 40000;
	private int previousFontSize;

	private static int fontSize = 3;
	static
	{	setFontSize( fontSize );
	}

	public LimitedSizeChatBuffer( String title )
	{
		super( title );
		previousFontSize = fontSize;

		this.highlights = new ArrayList();
		this.dehighlights = new ArrayList();
	}

	/**
	 * Used to change the font size for all current chat buffers.  Note that
	 * this does not affect logging.
	 */

	public static void setFontSize( int fontSize )
	{
		LimitedSizeChatBuffer.fontSize = fontSize;
		ChatBuffer.BUFFER_STYLE = "body { font-family: sans-serif; font-size: ";

		switch ( fontSize )
		{
			case 7:
				ChatBuffer.BUFFER_STYLE += "xx-large";
				break;

			case 6:
				ChatBuffer.BUFFER_STYLE += "x-large";
				break;

			case 5:
				ChatBuffer.BUFFER_STYLE += "large";
				break;

			case 4:
				ChatBuffer.BUFFER_STYLE += "medium";
				break;

			case 3:
				ChatBuffer.BUFFER_STYLE += "small";
				break;

			case 2:
				ChatBuffer.BUFFER_STYLE += "x-small";
				break;

			case 1:
				ChatBuffer.BUFFER_STYLE += "xx-small";
				break;

			default:
				ChatBuffer.BUFFER_STYLE += "100%";
				break;
		}

		ChatBuffer.BUFFER_STYLE += "; } a { color: black; text-decoration: none; } .pid1 { color: violet; }";
	}

	public static void setChatColors( String colorSetting )
	{
		if ( colorSetting == null )
			return;

		String [] colorArray = colorSetting.split( "[;:]" );

		setFontSize( fontSize );
		for ( int i = 0; i < colorArray.length; ++i )
			ChatBuffer.BUFFER_STYLE += " .pid" + colorArray[i] + " { color: " + colorArray[++i] + "; }";

		fontSize = 0 - fontSize;
	}

	/**
	 * Appends the given message to the chat buffer.
	 * @param	message	The message to be appended to this <code>ChatBuffer</code>
	 */

	public void append( String message )
	{
		boolean requiresUpdate = false;

		if ( previousFontSize != fontSize )
		{
			if ( fontSize < 0 )
				fontSize = 0 - fontSize;

			requiresUpdate = true;
		}

		// Now a test to see if you've already overflowed.
		// The scrollback option, when compared to the
		// alternative of obsessive memory usage, is a
		// trivial addition.  Therefore, when it hits
		// the buffer limit, trim it down to 5% of its
		// existing length.

		if ( displayBuffer.length() > BUFFER_LIMIT )
		{
			int trimIndex = displayBuffer.indexOf( "<br>" );
			while ( displayBuffer.length() - trimIndex > 10000 )
				trimIndex = displayBuffer.indexOf( "<br>", trimIndex + 1 );

			displayBuffer.delete( 0, trimIndex );
			requiresUpdate = true;
		}

		if ( requiresUpdate )
			fireBufferChanged( CONTENT_CHANGE, null );

		String highlightMessage = message;

		if ( !highlights.isEmpty() )
		{
			Pattern highlight, dehighlight;  Matcher matching;
			Iterator highlightIterator = highlights.iterator();
			Iterator dehighlightIterator = dehighlights.iterator();

			while ( highlightIterator.hasNext() )
				highlightMessage = applyHighlight( highlightMessage, (Pattern) highlightIterator.next(), (Pattern) dehighlightIterator.next() );
		}

		super.append( highlightMessage );

		if ( message.compareToIgnoreCase( highlightMessage ) != 0 )
			highlightBuffer.append( highlightMessage + "<br>" );

		previousFontSize = fontSize;
	}

	public void highlight( String highlight, LimitedSizeChatBuffer highlightBuffer )
	{
		if ( this == highlightBuffer )
			return;

		this.highlightBuffer = highlightBuffer;

		highlights.add( Pattern.compile( highlight, Pattern.CASE_INSENSITIVE ) );
		dehighlights.add( Pattern.compile( "href=\"([^\"]*)<font color=purple>" + highlight + "</font>", Pattern.CASE_INSENSITIVE ) );

		applyHighlights();
	}

	public void applyHighlights()
	{
		Pattern highlight, dehighlight;  Matcher matching;
		String highlightMessage;

		String displayString = displayBuffer.toString();
		String [] lines = displayBuffer.toString().split( "<br>" );

		for ( int j = 0; j < highlights.size(); ++j )
		{
			highlight = (Pattern) highlights.get(j);
			dehighlight = (Pattern) dehighlights.get(j);

			for ( int i = 0; i < lines.length; ++i )
			{
				highlightMessage = applyHighlight( lines[i], highlight, dehighlight );
				if ( lines[i].compareToIgnoreCase( highlightMessage ) != 0 )
					highlightBuffer.append( highlightMessage );
			}

			displayString = applyHighlight( displayString, highlight, dehighlight );
		}

		displayBuffer.setLength( 0 );
		displayBuffer.append( displayString );
		fireBufferChanged( CONTENT_CHANGE, null );
	}

	private String applyHighlight( String message, Pattern highlight, Pattern dehighlight )
	{
		Matcher matching = highlight.matcher( message );
		String highlightMessage = matching.replaceAll( "<font color=purple>" + highlight.pattern() + "</font>" );

		matching = dehighlight.matcher( highlightMessage );
		if ( matching.find( 0 ) )
		{
			String part1 = highlightMessage.substring( 0, matching.start() );
			String part2 = highlightMessage.substring( matching.start() ).replaceFirst( matching.group(), "href=\"" + matching.group(1) + highlight.pattern() );
			highlightMessage = part1 + part2;
			matching = dehighlight.matcher( highlightMessage );
		}

		return highlightMessage;
	}

}