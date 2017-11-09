#!/usr/bin/perl

# this script makes the git branch and moves/renames the package name for the playstore
# "Clean Project" and test!
use warnings;
use strict;
use File::Basename;


my $gitbranch = "playstore73";

my $from_pack = "com.quaap.launchtime";
my $to_pack   = "com.quaap.launchtime_official";

my $basedir = ".";

if (@ARGV>0) {

    if (@ARGV!=1 || @ARGV!=3 || @ARGV!=4) {
        die "Usage: $0 [gitbranch [from_package to_package [directory]]]";
    }

    if (@ARGV>=1) {
        $gitbranch = $ARGV[0];
    }

    if (@ARGV>=3) {
        $from_pack = $ARGV[1];
        $to_pack   = $ARGV[2];
    }

    if (@ARGV==4) {
        $basedir =   $ARGV[3];
        chdir $basedir;
    }
}

my @java_paths = (
 "app/src/main/java",
 "app/src/androidTest/java",
 "app/src/test/java"
);

my @skip = (
   basename($0),

   "build",
   "assets",
   "captures",
   ".git",
   ".idea",
   "README.md",
   "packages1.txt",
   "packages2.txt",
   "submitted_activities.txt",
   "submitted_packages.txt"
);



#Check if there are tracked changes
my @out = `git status -uno -s`;

die "git changes found!\n@out\n" if @out>0;


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



sub look_dir {
   my $ldir = shift;
   opendir D, $ldir;
   my @subs = readdir(D);
   closedir(D);
   
   for my $file (@subs) {
      
      my $ffile = "$ldir/$file";

      my $repath = $ffile;
      $repath=~s{^\Q$basedir\E/?}{};
      if ( grep( /^\Q$file\E|\Q$repath\E$/, @skip ) ) { next; }
      
      if (-d $ffile and $file ne "." and $file ne "..") {
         
         look_dir($ffile);
         
      } elsif (-T $ffile) {
          #  print "$ffile\n";
         open F, '<:utf8', $ffile;
         my @lines = <F>;
         close F;
         
         my $mod = 0; 
         for (@lines) {
            if (s/\b\Q$from_pack\E\b/$to_pack/g) {
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


if (system("git branch $gitbranch")!=0) {
    die "Couldn't make branch $gitbranch";
}
print "created branch $gitbranch\n";

if (system("git checkout $gitbranch")!=0) {
    die "Couldn't checkout branch $gitbranch";
}

for my $jpath (@java_paths) {
   my $dir = "$basedir/$jpath";
   
   my $cmd = qq(git mv "$dir/$from_pack_dir" "$dir/$to_pack_dir");
   print "$cmd\n";
   if (system($cmd)!=0) {
      die "Couldn't $cmd";
   }
   #print("move \"$dir/$from_pack_dir\",\"$dir/$to_pack_dir\"\n");
   #move("$dir/$from_pack_dir","$dir/$to_pack_dir");
   #system qq(git mv "$dir/$from_pack_dir" "$dir/$to_pack_dir");
}

look_dir($basedir);
