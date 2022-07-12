package edu.tum.sse.dirts.util;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class IgnoreErrorHandler implements ErrorHandler {

    @Override
    public void warning(SAXParseException e) throws SAXException {

    }

    @Override
    public void error(SAXParseException e) {

    }

    @Override
    public void fatalError(SAXParseException e) {

    }
}