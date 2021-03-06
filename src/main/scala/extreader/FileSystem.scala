package extreader

object FileSystem {
	def apply(bytes: Bytes) = new FileSystem(bytes, Superblock.in(bytes), None )
}

class FileSystem(val bytes: Bytes, sb: Superblock, val clean: Option[Bytes]) {
	var superblock : Option[Superblock] = None

	val blockCache = collection.mutable.Map[Long, Block]()
	val groupCache = collection.mutable.Map[Long, Group]()
	val inodeCache = collection.mutable.Map[Long, Inode]()

	var groupDescPad = 0

	def blockSizeExpo = sb.logBlockSize

	def blocksPerGroup = sb.blocksPerGroup // ext2: 8192 // 8 * blockSize ?

	def inodesPerGroup = sb.inodesPerGroup // ext2: 1832 // ext3: 1920

	def inodeCount = sb.inodeCount // ext2: 12824 // ext3: 19200
	def blockCount = sb.blockCount // ext2: 51200 // ext3: 76800

	def inodeBlocksPerGroup = sb.inodesPerGroup // ext2: 229

	def firstDataBlock = sb.firstBlock

	def groups = blockCount / blocksPerGroup

	def blockSize = 1024 << blockSizeExpo

	def inodesPerBlock = blockSize / inodeSize

	def intsPerBlock = blockSize / 4

  def inodeSize = sb.inodeSize

	def groupDescBlock = {
		if(blockSize == 1024 ) 
			metaBlock(2)
		else
			metaBlock(1)
	}

	val gdt = new GroupDescTable(groupDescBlock)

	def block(num: Long): Block = { 
		debug("[fs]\tfetch block "+num)
		blockCache.getOrElseUpdate(num, blockAt(num, num * blockSize))
	}

	def inode(num: Long) = inodeCache.getOrElseUpdate(num, loadInode(num))

	def loadInode(num: Long): Inode = {
		if(num < 1 || num > inodeCount) {
			throw new IllegalArgumentException("Bad Inode Number: "+num)
		}
		debug("[fs]\tinode "+num+" is the "+inodeIndexInBlock(num)+"th inode in group "+groupNumOfInode(num))

		val inodeBytes = group(groupNumOfInode(num)).inodeBytes(inodeIndexInBlock(num))

		new Inode(this, num, inodeBytes)
	}

	def group(num: Long): Group = {
		debug("[fs]\tfetch group "+num)
		groupCache.getOrElseUpdate(num, new Group(this, num, gdt(num) ))
	}

	def groupNumOfBlock(blockNum: Long) = (( blockNum - firstDataBlock ) / blocksPerGroup)
	def groupNumOfInode(inodeNum: Long) = (( inodeNum - 1) / inodesPerGroup)
	def inodeIndexInBlock(inodeNum: Long) = ( (inodeNum - 1) % inodesPerGroup )
		
	def blockAt(num: Long, at: Long) = {
		debug("[fs]\tblock "+num+" at "+at)
		new Block(num, bytes.getRange(at, blockSize) )
	}

	def metaBlock(num: Long): Block = {
		metaBlockAt(num, num * blockSize)
	}

	def metaBlockAt(num: Long, at: Long) = {
		new Block(num, clean.getOrElse(bytes).getRange(at, blockSize))
	}

	def fakeInodeAt(at: Long) = new Inode( this, -1, bytes.getRange(at, inodeSize))
}
