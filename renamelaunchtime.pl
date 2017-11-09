
# this file git moves/renames the package name for the playstore

my $gitbranch = "playstore73";

#Check if there are changes
my @out = `git status -uno -s`;

die "git changes found! Try 'git status -uno -s'" if @out>0;


#/**
# * Copyright (C) 2017   Tom Kliethermes
# *
# * This file is part of LaunchTime and is is free software; you can redistribute it and/or
# * modify it under the terms of the GNU General Public License as published by the
# * Free Software Foundation; either version 3 of the License, or (at your option) any
# * later version.
# *
# * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
# * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# * See the GNU General Public License for more details.
# */

use warnings;
use strict;



my $from_pack = "com.quaap.launchtime";
my $to_pack   = "com.quaap.launchtime_official";

my $basedir = ".";

my @skip = (
   "build",
   "assets",
   $0,
   "captures",
   ".git",
   ".idea",
   "README.md"
);


sub look_dir {
   my $ldir = shift;
   opendir D, $ldir;
   my @subs = readdir(D);
   closedir(D);
   
   for my $file (@subs) {
      
      if ( grep( /^$file$/, @skip ) ) { next; }
      
      my $ffile = "$ldir/$file";
      if (-d $ffile and $file ne "." and $file ne "..") {
         
         look_dir($ffile);
         
      } elsif (-T $ffile) {
          #  print "$ffile\n";
         open F, '<:utf8', $ffile;
         my @lines = <F>;
         close F;
         
         my $mod = 0; 
         for (@lines) {
            if (s/\b$from_pack\b/$to_pack/g) {
               $mod = 1;
            }
         }

         if ($mod) {
            print "$ffile\n";
            open F, '>:utf8', $ffile;
            
            for my $line (@lines) {
               print F $line;
            }
            close F;
         }
      }
   }
}


my $from_pack_dir = $from_pack;
my $to_pack_dir = $to_pack;

$from_pack_dir =~ s{\.}{/}g;
$to_pack_dir =~ s{\.}{/}g;

my @java_paths = (
 "app/src/main/java",
 "app/src/androidTest/java",
 "app/src/test/java"
);


system "git branch $gitbranch";

for my $jpath (@java_paths) {
   my $dir = "$basedir/$jpath";
   
   my $cmd = qq(git mv "$dir/$from_pack_dir" "$dir/$to_pack_dir");
   print "$cmd\n";
   system $cmd;
   #print("move \"$dir/$from_pack_dir\",\"$dir/$to_pack_dir\"\n");
   #move("$dir/$from_pack_dir","$dir/$to_pack_dir");
   #system qq(git mv "$dir/$from_pack_dir" "$dir/$to_pack_dir");
}

look_dir($basedir);
