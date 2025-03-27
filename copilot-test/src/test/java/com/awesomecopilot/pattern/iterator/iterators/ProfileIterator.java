package com.awesomecopilot.pattern.iterator.iterators;


import com.awesomecopilot.pattern.iterator.profile.Profile;

public interface ProfileIterator {
    boolean hasNext();

    Profile getNext();

    void reset();
}
