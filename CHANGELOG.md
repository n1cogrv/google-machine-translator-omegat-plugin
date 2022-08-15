# Change Log

## Add

## Changed

## Fixed

+ Method `getPreferenceName()` in machine translator plugin will fail the process of reading/parsing `omegat.prefs`
 when staring OmegaT, further, OmegaT will ignore said user config, post-fix it with `$(date).bak`, and create a 
 default and blank user config file. Fixed by alter return param in `getPreferenceName()` to a String with no space
 breaking.