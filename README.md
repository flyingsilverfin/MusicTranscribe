MusicTranscribe
===============

This is a project I started in 12th grade.
The original goal, without doing any research into the topic, was to create a mobile app that can listen to piano music and generate sheet music from it.

My approach essentially had two parts: an FFT and then using a large matrix reduction to calculate the linear combination of the FFT's of each note on the piano (which were prerecorded) that made up the new note just recorded. This required each note on the piano to be prerecorded into a large library. 

In the end, I stopped working on the project after a few months. I had reached the point where my approach worked somewhat when the range of the piano was restricted to one or two octaves on the keyboard, and the notes played were no more than a single note or a 2 note chord. There were several limiting factors to my approach: the FFT's inherent frequency response/sample time tradeoff, and especially the overtones of the dominant frequency of each note. Chords were especially problematic.

After doing a little research it turns out this is an active area of university research. Perhaps I will return to it once a suitable algorithm or approach has been developed and I will implement it here (or if I have a unique idea/approach!)
