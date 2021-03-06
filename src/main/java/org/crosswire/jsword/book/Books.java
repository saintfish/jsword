/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *       http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * Copyright: 2005-2013
 *     The copyright to this program is held by it's authors.
 *
 */
package org.crosswire.jsword.book;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.crosswire.common.activate.Activator;
import org.crosswire.common.util.CollectionUtil;
import org.crosswire.common.util.PluginUtil;
import org.crosswire.common.util.Reporter;
import org.crosswire.jsword.JSOtherMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Books class (along with Book) is the central point of contact between the
 * rest of the world and this set of packages.
 * 
 * @see gnu.lgpl.License for license details.<br>
 *      The copyright to this program is held by it's authors.
 * @author Joe Walker [joe at eireneh dot com]
 * @author DM Smith
 */
public final class Books extends AbstractBookList {
    /**
     * Create a singleton instance of the class. This is private to ensure that
     * only one can be created. This also makes the class final!
     */
    private Books() {
        super();
        books = new BookSet();
        drivers = new HashSet<BookDriver>();
    }

    /**
     * Accessor for the singleton instance
     * 
     * @return The singleton instance
     */
    public static Books installed() {
        return instance;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookList#getBooks()
     */
    public synchronized List<Book> getBooks() {
        return new BookSet(books);
    }

    /**
     * Search for the book by name. Looks for exact matches first,
     * then searches case insensitive. If that doesn't work, matches
     * the initials of the book first case sensitive, then insensitive.
     * In all cases the whole name or the whole initials has to match.
     * 
     * @param name The book to find
     * @return the book or null
     */
    public synchronized Book getBook(String name) {
        if (name == null) {
            return null;
        }

        // Check name first
        // First check for exact matches
        for (Book book : books) {
            if (name.equals(book.getName())) {
                return book;
            }
        }

        // Next check for case-insensitive matches
        for (Book book : books) {
            if (name.equalsIgnoreCase(book.getName())) {
                return book;
            }
        }

        // Then check initials
        // First check for exact matches
        for (Book book : books) {
            BookMetaData bmd = book.getBookMetaData();
            if (name.equals(bmd.getInitials())) {
                return book;
            }
        }

        // Next check for case-insensitive matches
        for (Book book : books) {
            if (name.equalsIgnoreCase(book.getInitials())) {
                return book;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookList#getBooks(org.crosswire.jsword.book.BookFilter)
     */
    @Override
    public synchronized List<Book> getBooks(BookFilter filter) {
        List<Book> temp = CollectionUtil.createList(new BookFilterIterator(getBooks(), filter));
        return new BookSet(temp);
    }

    /**
     * Get the maximum string length of a property
     * 
     * @param propertyKey
     *            The desired property
     * @return -1 if there is no match, otherwise the maximum length.
     */
    public int getMaxLength(String propertyKey) {
        int max = -1;
        for (Book book : getBooks()) {
            Object property = book.getProperty(propertyKey);
            if (property != null) {
                String value = property instanceof String ? (String) property : property.toString();
                max = Math.max(max, value.length());
            }
        }
        return max;
    }

    /**
     * Get the maximum string length of a property on a subset of books.
     * 
     * @param propertyKey
     *            The desired property
     * @param filter
     *            The filter
     * @return -1 if there is no match, otherwise the maximum length.
     */
    public int getMaxLength(String propertyKey, BookFilter filter) {
        int max = -1;
        for (Book book : getBooks(filter)) {
            Object property = book.getProperty(propertyKey);
            if (property != null) {
                String value = property instanceof String ? (String) property : property.toString();
                max = Math.max(max, value.length());
            }
        }
        return max;
    }

    /**
     * Add a Book to the current list of Books. This method should only be
     * called by BibleDrivers, it is not a method for general consumption.
     */
    public synchronized void addBook(Book book) {
        // log.debug("registering book: "+bmd.getName());

        books.add(book);
        fireBooksChanged(instance, book, true);
    }

    /**
     * Remove a Book from the current list of Books. This method should only be
     * called by BibleDrivers, it is not a method for general consumption.
     */
    public synchronized void removeBook(Book book) throws BookException {
        // log.debug("unregistering book: {}", bmd.getName());

        Activator.deactivate(book);

        boolean removed = books.remove(book);
        if (removed) {
            fireBooksChanged(instance, book, false);
        } else {
            throw new BookException(JSOtherMsg.lookupText("Could not remove unregistered Book: {0}", book.getName()));
        }
    }

    /**
     * Register the driver, adding its books to the list. Any books that this
     * driver used, but not any more are removed. This can be called repeatedly
     * to re-register the driver.
     * 
     * @param driver
     *            The BookDriver to add
     */
    public synchronized void registerDriver(BookDriver driver) throws BookException {
        log.debug("begin registering driver: {}", driver.getClass().getName());

        drivers.add(driver);

        // Go through all the books and add all the new ones.
        // Remove those that are not known to the driver, but used to be.
        Book[] bookArray = driver.getBooks();
        Set<Book> current = CollectionUtil.createSet(new BookFilterIterator(getBooks(), BookFilters.getBooksByDriver(driver)));

        for (int j = 0; j < bookArray.length; j++) {
            Book b = bookArray[j];
            if (current.contains(b)) {
                // Since it was already in there, we don't add it.
                // By removing it from current we will be left with
                // what is not now known by the driver.
                current.remove(b);
            } else {
                addBook(bookArray[j]);
            }
        }

        // Remove the books from the previous version of the driver
        // that are not in this version.
        for (Book book : current) {
            removeBook(book);
        }

        log.debug("end registering driver: {}", driver.getClass().getName());
    }

    /**
     * Remove from the list of drivers
     * 
     * @param driver
     *            The BookDriver to remove
     */
    public synchronized void unregisterDriver(BookDriver driver) throws BookException {
        log.debug("begin un-registering driver: {}", driver.getClass().getName());

        Book[] bookArray = driver.getBooks();
        for (int j = 0; j < bookArray.length; j++) {
            removeBook(bookArray[j]);
        }

        if (!drivers.remove(driver)) {
            throw new BookException(JSOtherMsg.lookupText("Could not remove unregistered Driver: {0}", driver.getClass().getName()));
        }

        log.debug("end un-registering driver: {}", driver.getClass().getName());
    }

    /**
     * Since Books keeps a track of drivers itself, including creating them when
     * registered it can be hard to get a hold of the current book driver. This
     * method gives access to the registered instances.
     */
    public synchronized BookDriver[] getDriversByClass(Class<? extends BookDriver> type) {
        List<BookDriver> matches = new ArrayList<BookDriver>();
        for (BookDriver driver : drivers) {
            if (driver.getClass() == type) {
                matches.add(driver);
            }
        }

        return matches.toArray(new BookDriver[matches.size()]);
    }

    /**
     * Get an array of all the known drivers
     * 
     * @return Found int or the default value
     */
    public synchronized BookDriver[] getDrivers() {
        return drivers.toArray(new BookDriver[drivers.size()]);
    }

    /**
     * Get an array of all the known drivers
     * 
     * @return Found int or the default value
     */
    public synchronized BookDriver[] getWritableDrivers() {
        int i = 0;
        for (BookDriver driver : drivers) {
            if (driver.isWritable()) {
                i++;
            }
        }

        BookDriver[] reply = new BookDriver[i];

        i = 0;
        for (BookDriver driver : drivers) {
            if (driver.isWritable()) {
                reply[i++] = driver;
            }
        }

        return reply;
    }

    /**
     * Registers all the drivers known to the program.
     */
    private void autoRegister() {
        // This will classload them all and they will register themselves.
        Class<? extends BookDriver>[] types = PluginUtil.getImplementors(BookDriver.class);

        log.debug("begin auto-registering {} drivers:", Integer.toString(types.length));

        for (int i = 0; i < types.length; i++) {
            // job.setProgress(Msg.JOB_DRIVER.toString() +
            // ClassUtils.getShortClassName(types[i]));

            try {
                Method driverInstance = types[i].getMethod("instance", new Class[0]);
                BookDriver driver = (BookDriver) driverInstance.invoke(null, new Object[0]); // types[i].newInstance();
                registerDriver(driver);
            } catch (NoSuchMethodException e) {
                Reporter.informUser(Books.class, e);
            } catch (IllegalArgumentException e) {
                Reporter.informUser(Books.class, e);
            } catch (IllegalAccessException e) {
                Reporter.informUser(Books.class, e);
            } catch (InvocationTargetException e) {
                Reporter.informUser(Books.class, e);
            } catch (BookException e) {
                Reporter.informUser(Books.class, e);
            }
        }
    }

    /**
     * The list of Books
     */
    private BookSet books;

    /**
     * An array of BookDrivers
     */
    private Set<BookDriver> drivers;

    /**
     * The log stream
     */
    private static final Logger log = LoggerFactory.getLogger(Books.class);

    /**
     * The singleton instance.
     * This needs to be declared after all other statics it uses.
     */
    private static final Books instance = new Books();
    // And it cannot register books until it is fully constructed
    // When this was the last call in the constructor it resulted
    // in "instance" being null in something it called.
    static {
        instance.autoRegister();
    }
}
