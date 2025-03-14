package com.copilot.pattern.iterator.iterators;


import com.copilot.pattern.iterator.profile.Profile;

public interface ProfileIterator {
    boolean hasNext();

    Profile getNext();

    void reset();
}
