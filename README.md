		Do I have a superblock?
			Yes:


			No:





Notes:

	Types:
		BlockNum (which is just an alias for Long) should only be used to identify absolute block numbers, not offsets
		
		




Possible method of figuring out a trashed FS:

1) Find root dir listing 
	find a dir rec by scanning for their pattern in raw bytes
	Scan for rootdir's contents (it'll be in a datablock):

	You can recognize the root dir's contents by the presence of two sequenctial records for '.' and '..' both pointing to inode 2. You now know that the first record is the begining of the first data block for inode 2.

	Could be used on other dirs too: inode in entry for . points to that rec.


2) Find inode 2
	By searching for an inode-like thing which points to the dir rec location obtained above, one finds the inode
	The dir rec points to inode num -- you can then find that inode by scanning for an inode pattern pointing to the offset where you found the dir rec.

3) Step back inode_length (128) and you're at inode 1.

4) find the address of first inode in group desc table
