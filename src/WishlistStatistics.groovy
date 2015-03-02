import groovy.xml.MarkupBuilder

if( args.size() != 2 ){
	println "Usage : groovy WishlistStatistics.groovy <Item List File to Read> <Threshold Count for pickup(Num)>"
	return
}


def itemCount = [:].withDefault{ 0 }
def itemUrl = [:]
def itemNum = 0

//read
new File( args[0] ).eachLine{ l->
	def sp = l.split(/\t/)
	if( sp.size() != 3 ){
		println l+" "+sp
		return
	}
	def n = sp[0].replaceAll(/\p{C}/,'')
	def u = sp[1]

	itemCount[ n ]++
	itemUrl[ n ] = u
	itemNum++
}

//out
String dateStr = new Date().format(/yyyyMMddHHmm/)
new File( "out_ItemCount_${dateStr}.txt" ).withWriter{ w->
	w.println "Total Item num : "+itemNum
	w.println "Total Item kind num : "+itemCount.keySet().size()
	itemCount.sort{ it.value }.each{
		w.println it.value+"\t"+it.key+"\t"+itemUrl[ it.key ]
	}
}


//make pickupItems html
def countThreshold = args[1] as int
def pickupItems = itemCount.findAll{ it.value >= countThreshold }.sort{ it.value }
def mb = new MarkupBuilder( new FileWriter("PickupItems.html") )
mb.('!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"'){}
mb.html{
	head{
		title( dateStr )
		link(rel:"stylesheet", href:"main.css", type:"text/css", media:"all", " ")
	}
	body{
		h1("${pickupItems.size()} items"){}
		h1("Count : ItemName"){}
		pickupItems.each { v->
			a(href:itemUrl[ v.key ]){
				h2( v.value+"\t:\t"+v.key ){}
			}
		}
	}
}

//misc
//count variation
def countKind = [:].withDefault{ 0 }
itemCount.each{
	countKind[ it.value ]++
}
println "item count kind"
countKind.sort{it.key}.each{
	println "count\t"+it.key+"\t"+it.value
}




