JAVACFLAGS = -cp "./libraries/*"
JAVAC = javac ${JAVACFLAGS}
FLAGS = -g -Xlint:deprecation

# Main java files, not including the 3D stuff
SRCS = \
seq/*.java \
seq/engine/*.java \
seq/gui/*.java \
seq/util/*.java \
seq/motif/blank/*.java \
seq/motif/blank/gui/*.java \
seq/motif/macro/*.java \
seq/motif/macro/gui/*.java \
seq/motif/automaton/*.java \
seq/motif/automaton/gui/*.java \
seq/motif/notes/*.java \
seq/motif/notes/gui/*.java \
seq/motif/parallel/*.java \
seq/motif/parallel/gui/*.java \
seq/motif/select/*.java \
seq/motif/select/gui/*.java \
seq/motif/series/*.java \
seq/motif/series/gui/*.java \
seq/motif/stepsequence/*.java \
seq/motif/stepsequence/gui/*.java \
seq/motif/silence/*.java \
seq/motif/silence/gui/*.java \
seq/motif/arpeggio/*.java \
seq/motif/arpeggio/gui/*.java \
seq/motif/filter/*.java \
seq/motif/filter/gui/*java \

#seq/motif/modulation/*.java \
#seq/motif/modulation/gui/*.java 

# Make the main Seq code
all:
	@ echo This makes the Seq code.
	@ echo To learn about other options, type 'make help'
	@ echo
	${JAVAC} ${SRCS}

# Delete all jmf gunk, checkpoints, backup emacs gunk classfiles,
# documentation, and odd MacOS X poops
clean:
	find . -name "*.class" -exec rm -f {} \;
	find . -name "jmf.log" -exec rm -f {} \;
	find . -name ".DS_Store" -exec rm -f {} \; 
	find . -name "*.checkpoint" -exec rm -f {} \;
	find . -name "*.java*~" -exec rm -f {} \;
	find . -name ".#*" -exec rm -rf {} \;
	rm -rf target/*.jar docs/classdocs/resources docs/classdocs/ec docs/classdocs/sim docs/classdocs/*.html docs/classdocs/*.css docs/classdocs/package*
	rm -rf ${CLASSROOT}
	rm -rf target/

# Build a jar file.  Note this collects ALL .class, .png, .jpg, index.html, and simulation.classes
# files.  you'll probably want to strip this down some.
jar: all
	- mkdir install
	rm -rf install/seq.jar uk META-INF com org module-info.class
	touch /tmp/manifest.add
	rm /tmp/manifest.add
	echo "Main-Class: seq.gui.SeqUI" > /tmp/manifest.add
	cd libraries ; jar -xvf coremidi4j-1.6.jar ; jar -xvf json.jar ; jar -xvf flatlaf-3.4.1.jar
	mv libraries/META-INF . ; mv libraries/uk . ; mv libraries/com . ; mv libraries/org . 
	jar -cvfm install/seq.jar /tmp/manifest.add `find seq -name "*.class"` `find seq -name "*.html"` `find seq -name "*.png"` `find seq -name "*.jpg"` uk/ META-INF/ org/ com/ 
	echo jar -cvfm install/seq.jar `find seq -name "*.class"` `find seq -name "*.html"` `find seq -name "*.png"` `find seq -name "*.jpg"` uk/ META-INF/ org/ com/ 
	rm -rf uk META-INF com org module-info.class

install8: jar
	rm -rf install/Seq.app install/bundles install/Seq.dmg.html install/Seq.dmg.jnlp
	- javapackager -deploy -native dmg -srcfiles install/seq.jar -appclass seq.gui.SeqUI -name Seq -outdir install -outfile Seq.dmg -v
	- mv install/bundles/Seq-1.0.dmg install/Seq.dmg
	rm -rf install/bundles install/Seq.dmg.html install/Seq.dmg.jnlp 

install: jar
	rm -rf install/Seq.app install/bundles install/Seq.dmg.html install/Seq.dmg.jnlp
	- jpackage --input install --name Seq --main-jar seq.jar --main-class seq.gui.SeqUI --type dmg --mac-package-name "Seq" --verbose --java-options '-XX:+UseZGC -XX:MaxGCPauseMillis=1'
	open Seq-1.0.dmg


# Indent to your preferred brace format using emacs.  Seq's default
# format is Whitesmiths at 4 spaces.  Yes, I know.  Idiosyncratic.
# Anyway, beware that this is quite slow.  But it works!
indent: 
	touch ${HOME}/.emacs
	find . -name "*.java" -print -exec emacs --batch --load ~/.emacs --eval='(progn (find-file "{}") (mark-whole-buffer) (setq indent-tabs-mode nil) (untabify (point-min) (point-max)) (indent-region (point-min) (point-max) nil) (save-buffer))' \;


# Print a help message
help: 
	@ echo Seq Makefile options
	@ echo 
	@ echo "make          Builds the model core, utilities, and 2D code/apps only"
	@ echo "make all      (Same thing)"
	@ echo "make 3d       Builds the model core, utilities, and both 2D and 3D code/apps"
	@ echo "make docs     Builds the class documentation, found in docs/classsdocs"
	@ echo "make doc      (Same thing)"
	@ echo "make clean    Cleans out all classfiles, checkpoints, and various gunk"
	@ echo "make jar      Makes, then collects ALL class files into a jar file"
	@ echo "              called" target/seq.${VERSION}.jar
	@ echo "make help     Brings up this message!"
	@ echo "make indent   Uses emacs to re-indent Seq java files as you'd prefer"
	@ echo

