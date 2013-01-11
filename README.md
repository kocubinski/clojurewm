# clojurewm

Sick and tired of having no suitable window manager while working in Microsoft
Windows.

Inspired by xmonad, scrotwm, dwm, windawesome, Actual Window Manager,
python-windows-tiler, bug.n.

Built with [clojure-clr](https://github.com/clojure/clojure-clr), and 
[ClojureClrEx](https://github.com/kocubinski/ClojureClrEx) for logging, pinvoke,
and type building.

## Motivation
There is no suitable way to easily organize and recall large numbers of windows
while working on Microsoft Windows. Alt-Tab fails, and moving a hand to the
mouse interrupts flow.  Support for virtual desktops in Windows is bad as well,
especially for multiple monitors.

What is really needed is a way to easily tag and recall windows on demand, free
from any concepts of virtual desktops, monitors or window layouts.

## Intent
clojurewm will:
* provide an easy way to tag windows with any hotkey combination at runtime.
* immediately focus a tagged window whenever this hotkey is entered.
* **not** require a configuration file to set window tags.
* **not** hide any windows (e.g. virtual desktops).
* **not** tile any windows (use http://winsplit-revolution.com/home).

## Usage
### Terms
**window** : any opened window  
**hotkey** : a key combination (e.g. Alt-1, Alt-Shift-T)  
**tag** : a hotkey to which a window or windows are assigned.

### Commands
#### Tag window
*Alt-Shift-T*  
Assign a hotkey tag to the currently focused window. The next key sequence
entered will used.  Entering the hotkey again will recall all windows tagged
with that hotkey.
    
#### Fullscreen window  
*Alt-Shift-F*  
Fullscreen the current window, hiding and borders and title bar. Still in alpha,
doesn't work propertly for all applications, and behaves oddly on multiple
monitors.

#### Next Window
*Alt-J*
Switch to the next window in the currently active tag.

#### Previous Window
*Alt-K*
Switch to the previous window in the currently active tag.

#### Activate Tag
*<user defined>*
Activate tag and recall (bring to foreground) all windows with this tag. 

#### Quit
*Alt-Shift-Q*  
Quit clojurewm.

## Getting clojurewm
Download the latest release package here: https://dl.dropbox.com/u/101986306/clojurewm-0.02.zip  
To start clojurewm run `clojurewm.run.exe`  
To start clojurewm with a REPL run `clojurewm.console.exe --clj` from cmd.

## TODO
* Configurable hotkey assignment hotkey for commands.
* Command to display current tag list.
* Bugs?
