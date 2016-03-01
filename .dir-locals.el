;; The 'nil' configuration applies to all modes.
((nil . ((compile-command . "cd ~/src/android/library && ant clean && ant debug && adb install -r bin/Library-debug.apk")))
 (sgml-mode . ((indent-tabs-mode . t)
	 (tab-width . 4))))
