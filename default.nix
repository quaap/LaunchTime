let
  stablePkgs = import <nixpkgs> {};
  android-nixpkgs = import <android-nixpkgs> {};  

  android-sdk = android-nixpkgs.sdk (sdkPkgs: with sdkPkgs; [
    cmdline-tools-latest
    build-tools-35-0-1
    platform-tools
    platforms-android-35
    emulator
  ]);


in stablePkgs.stdenv.mkDerivation {
  name = "androidasasd-env";
  
  buildInputs = [
    stablePkgs.apktool
    stablePkgs.git
    stablePkgs.jdk17
    stablePkgs.unzip
    stablePkgs.jq
    android-sdk
  ];
  
  
  SOURCE_DATE_EPOCH = 315532800;
  PROJDIR = "${toString ./.}";
	S_USB_DEVICE="Pixel";
  
  shellHook = ''
      export _ANDROID_JAR=${android-sdk}/share/android-sdk/platforms/android-35/android.jar
      export PATH=$PATH:${android-sdk}/share/android-sdk/build-tools/35.0.1/
  '';
}


