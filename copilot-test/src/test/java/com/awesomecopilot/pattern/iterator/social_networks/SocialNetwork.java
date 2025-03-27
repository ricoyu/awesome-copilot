package com.awesomecopilot.pattern.iterator.social_networks;


import com.awesomecopilot.pattern.iterator.iterators.ProfileIterator;

public interface SocialNetwork {
    ProfileIterator createFriendsIterator(String profileEmail);

    ProfileIterator createCoworkersIterator(String profileEmail);
}
