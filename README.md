# Seq  
A Modular, Hierarchical Music Sequencer

Version 1

By Sean Luke (sean@cs.gmu.edu)  
With Help from Filippo Carnovalini (filippo.carnovalini@vub.be)  
Copyright 2024 by Sean Luke and George Mason University

Related projects:  

* [Edisyn](https://github.com/eclab/edisyn), a patch editor toolkit with sophisticated exploration tools.
* [Flow](https://github.com/eclab/flow), a fully-modular, polyphonic, additive software synthesizer.
* [Gizmo](https://cs.gmu.edu/~sean/projects/gizmo/), an Arduino-based MIDI Swiss Army knife.
* [*Computational Music Synthesis*](https://cs.gmu.edu/~sean/book/synthesis/), an open-content book on building software synthesizers.

## Donations

Donations are welcome via Paypal to Sean's address (sean@cs.gmu.edu).

## About

Seq is a very unusual music sequencer.  In Seq, you write chunks of music, then put combine and modify them in certain ways, then combine the combinations, and so on, until you reach a final song. Seq has many ways to combine stuff, everything from simple ordered series to complex networks (automata).

Seq is written in pure Java.  It runs on MacOS, Linux, and Windows.

This is an early release of Seq, and it's got lots of wires sticking out here and there, is missing important items, and has bugs.  And you have to build it [we'll get binaries latter].  But it works!

## Resources

* Seq has an [initial, rudimentary manual](https://cs.gmu.edu/~eclab/projects/seq/seq.pdf) which explains the basics of using it.

* Interested in helping out on Seq?  Get ahold of us!

## Install and Run Seq

Seq has to be built from source for the time being:

1. Install the libraries found in the **libraries** folder
2. Go into the seq directory and run **make**
3. You can run Seq as **java seq.gui.SeqUI**

Seq should probably run on Windows, MacOS, and Linux, but we have primarily developed it on MacOS.
