#  *Musify!*

### *Musify!* is a versatile client-side mod designed to improve the immersion and auditory experience of Minecraft for both modpack developers and regular players. This mod allows for dynamic music customization based on in-game events like PvE Combat, Biome Exploration, Caving, and more, giving you complete control over Minecraft's soundtrack. Whether you’re crafting an immersive modpack or just want to personalize your solo adventures, *Musify!* provides a way to bring your world to life with the perfect soundtrack.

## Features
This mod has a bunch of different features revolving around Minecraft's soundtrack:
- **Customizable Biome Music!** This Mod allows you to customize and set up music for specific biomes, or for certain biome types. Great for that immersive feel when exploring!
- **Combat Music!** Enhance your battle experience by setting up custom combat music!
- **Cavern Music!** Want to feel a true Dwarf when mining underground? Customize your worlds underground music for a new experience when mining!
- **Boss Music!** Customize music for boss mobs for a truely unique experience!
- **Main Menu Music!** Set up some custom music for Minecraft's main menu to personalize your modpack or client!
- **Ambient Mode!** Want to use sound effects or ambient tracks instead of music? Enable ambient mode to add to vanilla music instead of replacing it!

## How to use:
1. The mod will take .ogg sound files from the folder called "biomemusic". This folder is located in the instance path (the same folder where your clients mod, resourcepack and config folders are.) which is usually your .minecraft folder, but can be a different folder depending on if you are using another launcher like curseforge. If this folder does not exist it will be created by the mod upon first startup. If you are a modpack developer that wants to use this mod you will have to mark this folder for export. once you have created the folder or once the mod has created the folder for you, you can put any .ogg sound files you need in the folder.

![folder PNG](https://github.com/user-attachments/assets/6f74f571-86af-4b90-a194-370bc8083aa7)

In the biomemusic folder, put the sound files. Make sure they are .ogg files!

![image](https://github.com/user-attachments/assets/e1c3f13d-3f9c-44c0-88c5-eff1e8559d73)


2. While it is possible to use the config file to set up the mod, it is recommended to use to in-game config menu in the forge mod list.

![image](https://github.com/user-attachments/assets/beeb4fcf-086d-4dfc-93c0-cceb6b1f58ae)
![image](https://github.com/user-attachments/assets/064b70ef-3142-46fe-85e2-4340733141e4)

By default Combat Music, Cavern Music and Boss Music are disabled. At the top of the config menu there will be a list called "Available Music Files", this can be used to see what sound files in the musify folder are recognized and can be used. For all music except boss music and the main menu music, you can specify more then one track by seperating them with a comma e.g. [song1.ogg,song2.ogg]. If ambient Mode is enabled custom music will not fade out and stop vanilla music. [The Biome Music Mapping] is used to specify custom music for individual biomes, this will overwrite the music set for tags of the biome. Setting music for a biome tag in the [Biome Tag Music Mapping] will play this music in all biomes with that tag. If a biome has more than one tag, a random song from any tag will play with slight priority set in the biomes tag order. For example, if you set up two music tracks for the dry and the savanna tags, it one of them randomly when you enter a savanna, but will more often choose a song from the savanna tag then from the dry tag, because savanna is the first tag for the savanna biome. If a custom song is already playing and you enter a new biome the song will not change if the song is specified in one of the tags for that biome, unless you specify a different song in the [Biome Music Mapping].

   For **Boss Music**, There is an option to set the detection range for bosses. If a boss is detected within that radius the music will change if the mob is specified as a boss mob. To set up the music for a boss mob there is the [Boss Music List]. For each boss mob you want to set up music for, create a new line and specify the mob and the music seperated by a comma: [modid:mobid,bossmusic1.ogg]. Keep in mind to not forget the Mod Id from the mob! (e.g. [lycanitesmobs:rahovart,bossmusic1.ogg]). The Boss Music has priority based on the index of the list. It will always try to play the muscic for the boss mob it can find first in the list. So if you have both the wither and the Ender Dragon in the list and you spawn in both of them, it will play the music from whichever mob is first in the list.

   For **Combat Music**, you can set the range in which it tries to look for aggro'd mobs, the amount of aggro'd mobs you need to start the combat music, and the amount of mobs that need to be left to stop the combat music. In The [Combat Music List] you can set general combat soundtracks, If there is no Linked SoundTrack specified in the [musiclink] option a random song from here will play when combat music engages.

   **MusicLink**. This option is used if you have two of the same soundstracks of which one is a combat track. To use this, look for the normal track in the list, and as the input give the combat variant of that track. This will make it so that if the music is playing it will always play the combat version of the track when combat music engages. If there is no musiclink specified, it will play a random song from the [Combat Music List] in the combat options.

   For **Cavern Music** you are able to set the starting Y level at which cavern music starts, and the Y level at which it stops. Please keep in mind to place the stop Y level sufficiently above the Start Y level as to not fade-in and fade-out the music all too much. You can set what cavern music you want in the [Cavern Music List:].

   **Fade Options** is the last part of the config. I cannot stress this enough, **If you do not know what you are doing, do not change these options.** Changing these option to bad values can and will break the mod. 
The fade-in and fade-out options control how fast or slow music fades in or out when changing, while you can set these to different values to customize the mod to your liking, please do not set them to excessively low or high values.
The [Polling Rate] option is the heart of the mod. every action the mod takes is controlled by the polling rate, which acts as a sort of timer. Increasing the polling rate value slow down how fast the mod reacts to changes and the other way around. In general it is recommended to only try higher values for a slower and less intrusive experience if you find that the music still changes too fast even after first setting up the rest of the mod completely.


## Disclaimer
- While the mod does work with *most* Modded entities and biomes, some might not work. The mod was tested with mods like DefiledLands and Lycanites, But some mobs from other mods with which the mod was not tested might not be compatible. Similarly for some custom biomes from other mods, they might not be recognized. Even if the mod does not recognize a biome, as long as they either have parent biomes or biome tags, the music set using biome tags will work.

- Please do not forget to add the .ogg to the sound files when setting up music. Without .ogg behind the file name it will not work!
- Please do not forget to set values back to "default_music" when you want to disable something. It might break certain functions if left empty.

- Yes, I will probably update this mod to later versions of minecraft if I have time or if the mod gains a lot of traction.
