import groovy.xml.MarkupBuilder
import java.net.URLEncoder
import java.net.URLDecoder

@Grapes( @Grab(group='nekohtml', module='nekohtml', version='0.7.6') )
@Grapes( @Grab(group='xerces', module='xercesImpl', version='2.11.0') )
import groovyx.gpars.GParsExecutorsPool


class AmazonWishGetter {

	final int SLEEP_TIME = 5000

	static final List USER_AGENTS= [
		"Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; ARM; Trident/6.0)",
		"Opera/9.80 (Macintosh; Intel Mac OS X 10.6.8; U; ja) Presto/2.10.289 Version/12.00",
	]

	List<String> getAmazonWishlistbyWord( String searchWord, int pageNum=1 ){
		sleep( SLEEP_TIME ) // sleep for peace ;)

		List ret = []

		final String userAgent = USER_AGENTS[new Random().nextInt( USER_AGENTS.size() )]
		final String hp = "http://www.amazon.co.jp/gp/registry/search/ref=cm_wl_search__sortbar_page_"+pageNum+"?ie=UTF8&field-firstname=&field-lastname=&index=jp-xml-wishlist&sortby=name&submit.search=1&type=wishlist&field-name="
		final String baseUrl = "http://www.amazon.co.jp/"

		def accessUrl = hp+searchWord+"&page="+pageNum

		println "wishlistURL : "+accessUrl

		XmlParser p =  new XmlParser( new org.cyberneko.html.parsers.SAXParser() )
		try{
			HttpURLConnection connect = (HttpURLConnection)(
					new URL( accessUrl ) ).openConnection();
			connect.setRequestMethod("GET");
			connect.setInstanceFollowRedirects(false);
			connect.setRequestProperty( "User-Agent", userAgent)
			connect.connect();


			Node doc = p.parse( new InputStreamReader( connect.getInputStream(), "utf-8" ) )

			doc.depthFirst().DIV.findAll{ it['@class']=="a-section a-spacing-none a-spacing-top-none" }.each{
				if( it.A['@href'] != [] )
					ret += baseUrl + it.A['@href'][0]
			}

			//get additional page no.
			def pageNo = []
			doc.depthFirst().LI.findAll{ it["@class"]=="a-" }.each{
				//println it
				pageNo += it["@data-wls-page-trigger"].replaceAll(/\{"page":/, '').replaceAll(/\}/, '') as int
			}
			// recursion
			println "pNo "+pageNo
			pageNo = pageNo.findAll{ it > pageNum }
			if( pageNo != [] ){
				ret += getAmazonWishlistbyWord( searchWord, pageNo[0] )
			}

		} catch( Exception e ){
			e.printStackTrace()
			//retry
			println "Error getting wishlists '${searchWord}' (${pageNum})... Sleep and Retry"
			sleep(10000)
			ret += getAmazonWishlistbyWord( searchWord, pageNum )
		}

		return ret
	}


	List getWishItems( String url, int pageNum = 1 ){
		sleep( SLEEP_TIME ) // sleep for peace ;)

		def accessUrl = url + "?page="+pageNum

		println "ItemPage : "+accessUrl

		final String userAgent = USER_AGENTS[new Random().nextInt( USER_AGENTS.size() )]
		List ret = []

		XmlParser p =  new XmlParser( new org.cyberneko.html.parsers.SAXParser() )
		try{
			HttpURLConnection connect = (HttpURLConnection)(
					new URL( accessUrl ) ).openConnection();
			connect.setRequestMethod("GET");
			connect.setInstanceFollowRedirects(false);
			connect.setRequestProperty( "User-Agent", userAgent)
			connect.connect();


			Node doc = p.parse( new InputStreamReader( connect.getInputStream(), "utf-8" ) )

			//get items
			doc.depthFirst().H5.each{
				def title = it.A['@title'][0]
				def itemUrl = "http://www.amazon.co.jp/"+it.A['@href'][0]
				if( title != null && itemUrl != null ){
					def formattedTitle = getFormattedItemName( title )
					ret += [[ formattedTitle, itemUrl, url ]]	//itemName, itemUrl, userUrl
				}

			}

			//get additional page no.
			def pageNo = []
			doc.depthFirst().LI.findAll{ it["@class"]=="a-" }.each{
				pageNo += it["@data-pag-trigger"].replaceAll(/\{"page":/, '').replaceAll(/\}/, '') as int
			}

			// recursion
			println "pNo "+pageNo
			pageNo = pageNo.findAll{ it > pageNum }
			if( pageNo != [] ){
				ret += getWishItems( url, pageNo[0] )
			}

		} catch( Exception e ){
			e.printStackTrace()
			//retry
			println "Error getting items '${url}' (${pageNum})... Sleep and Retry"
			sleep(10000)
			ret += getWishItems( url, pageNum )
		}

		return ret
	}

	def getFormattedItemName( String n ){
		return n.replaceAll(/\?/, '')
	}
}

def mainProcess(){

	//util
	def enc = { URLEncoder.encode(it, "utf-8") }
	def dec = { URLDecoder.decode(it, "utf-8") }
	def dateStr = new Date().format('yyyyMMddHHmm')

	def awg = new AmazonWishGetter()

	//decide search words
	def words = []
	new File(/SearchWords.txt/).eachLine{
		words += enc( it )
	}
	println "Search words are "+words.collectAll( dec )

	//get wishlists
	wishlistUrls = []
	words.each{
		println "-"*30
		println "Search for "+dec( it )
		wishlistUrls += awg.getAmazonWishlistbyWord( it )
	}

	//make wishlists unique
	wishlistUrlsUniq = wishlistUrls.unique( false )
	println "size wl:"+wishlistUrls.size()+" wlu:"+wishlistUrlsUniq.size()

	//output wishlist
	new File( "out_WishlistUrl_"+dateStr+".txt" ).withWriter{ w->
		w.println "Search words are "+words.collectAll( dec )
		wishlistUrlsUniq.each{
			w.println( it )
		}
	}


	//get item list
	itemList = []
	wishlistUrlsUniq.eachWithIndex{ it, i ->
		println "-"*30
		println "Get items("+i+"/"+wishlistUrls.size()+") of "+it
		itemList += awg.getWishItems( it )
	}

	//output item list
	new File( "out_ItemList_"+dateStr+".txt" ).withWriter{ w->
		itemList.findAll{ it[0] != null }.each{
			w.println( it[0]+"\t"+it[1]+"\t"+it[2] )
		}
	}

	println "Finish... total Wishlists : "+wishlistUrlsUniq.size()+" items : "+itemList.size()
}

mainProcess()


