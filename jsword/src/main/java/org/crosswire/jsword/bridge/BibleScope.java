/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 as published by
 * the Free Software Foundation. This program is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *       http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * Copyright: 2008
 *     The copyright to this program is held by it's authors.
 *
 * ID: $Id: BookIndexer.java 1466 2007-07-02 02:48:09Z dmsmith $
 */
package org.crosswire.jsword.bridge;

import java.util.Iterator;

import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.BookCategory;
import org.crosswire.jsword.book.Books;
import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.KeyFactory;
import org.crosswire.jsword.passage.PassageKeyFactory;
import org.crosswire.jsword.versification.BibleInfo;

/**
 * Determines the scope of the Bible.
 * That is, the verses that are in the Bible and the verses that are not.
 * This is based upon the KJV versification.
 * 
 * @see gnu.lgpl.License for license details.
 *      The copyright to this program is held by it's authors.
 * @author DM Smith [dmsmith555 at yahoo dot com]
 */
public class BibleScope
{

    public BibleScope(Book book)
    {
        this.book = book;
    }

    /**
     * Get a key containing all the verses that are in this Bible.
     * @return verses that are in scope
     */
    public Key getInScope()
    {
        computeScope();
        return inScope;
    }

    /**
     * Get a key containing all the verses that are not in this Bible.
     * @return verses that are out of scope
     */
    public Key getOutOfScope()
    {
        computeScope();
        return outScope;
    }

    private void computeScope()
    {
        if (inScope == null)
        {
            KeyFactory keyf = PassageKeyFactory.instance();
            Key all = keyf.getGlobalKeyList();
            inScope = keyf.createEmptyKeyList();
            outScope = keyf.createEmptyKeyList();
            Iterator iter = all.iterator();
            while (iter.hasNext())
            {
                Key key = (Key) iter.next();
                if (book.contains(key))
                {
                    inScope.addAll(key);
                }
                else
                {
                    outScope.addAll(key);
                }
            }
        }
    }

    private Book book;

    /**
     * Call with &lt;operation&gt; book.
     * Where operation can be one of:
     * <ul>
     * <li>check - returns "TRUE" or "FALSE" indicating whether the index exists or not</li>
     * <li>create - (re)create the index</li>
     * <li>delete - delete the index if it exists</li>
     * </ul>
     * And book is the initials of a book, e.g. KJV.
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            usage();
        }

        System.err.println("BibleScope " + args[0]); //$NON-NLS-1$

        Book b = Books.installed().getBook(args[0]);
        if (b == null)
        {
            System.err.println("Book not found"); //$NON-NLS-1$
            System.exit(1);
        }

        if (!b.getBookCategory().equals(BookCategory.BIBLE))
        {
            System.err.println(b.getInitials() + " is not a Bible"); //$NON-NLS-1$
            System.exit(1);
        }

        BibleScope scope = new BibleScope(b);
        BibleInfo.setFullBookName(false); // use short names
        System.out.println("Scope of KJV versification for " + b.getInitials()); //$NON-NLS-1$
        System.out.println("In scope     = " + scope.getInScope().getName()); //$NON-NLS-1$
        System.out.println("Out of scope = " + scope.getOutOfScope().getName()); //$NON-NLS-1$
    }

    public static void usage()
    {
        System.err.println("Usage: BibleScope book"); //$NON-NLS-1$
        System.exit(1);
    }

    private Key inScope;
    private Key outScope;
}