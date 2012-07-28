#!/usr/bin/env python
import urllib
import datetime
import re
import os
import sys

CALENDAR = [ 0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 ]
PAGES = 2

# params
year = int(sys.argv[1])
month = int(sys.argv[2])
set_day = int(sys.argv[3])
target_dir = sys.argv[4]

# photo_ids and titles are extracted from explore page source
link_pattern = """(?<=data-track="thumb" href=\"/photos/)(\w)*/(\w)*/"""
title_pattern = """(?<=title=")([\S\s])*(?=")"""

re_link = re.compile(link_pattern)
re_title = re.compile(title_pattern)

# ****************************************
# Flickr API Params
# ****************************************
HOST = 'http://flickr.com'
API = '/services/rest'
API_KEY = "e9c26bc8a4a20f2a15aab8ac659f25e2"
API_SECRET = "57940bdb914aa0a8"

# ***************************************
# Flickr API Helpers
# ***************************************
class Bag: pass

def safe_unicode(obj, *args):
    try:
        return unicode(obj, *args)
    except UnicodeDecodeError:
        ascii_text = str(obj).encode('string_escape')
        return unicode(ascii_text)

#unmarshal taken and modified from pyamazon.py
#makes the xml easy to work with
from xml.dom import minidom
from urllib import urlencode, urlopen

def unmarshal(element):
    rc = Bag()
    if isinstance(element, minidom.Element):
        for key in element.attributes.keys():
            setattr(rc, key, element.attributes[key].value)

    childElements = [e for e in element.childNodes \
                     if isinstance(e, minidom.Element)]
    if childElements:
        for child in childElements:
            key = child.tagName
            if hasattr(rc, key):
                if type(getattr(rc, key)) <> type([]):
                    setattr(rc, key, [getattr(rc, key)])
                setattr(rc, key, getattr(rc, key) + [unmarshal(child)])
            elif isinstance(child, minidom.Element) and \
                     (child.tagName == 'Details'):
                # make the first Details element a key
                setattr(rc,key,[unmarshal(child)])
                #dbg: because otherwise 'hasattr' only tests
                #dbg: on the second occurence: if there's a
                #dbg: single return to a query, it's not a
                #dbg: list. This module should always
                #dbg: return a list of Details objects.
            else:
                setattr(rc, key, unmarshal(child))
    else:
        #jec: we'll have the main part of the element stored in .text
        #jec: will break if tag <text> is also present
        text = "".join([e.data for e in element.childNodes \
                        if isinstance(e, minidom.Text)])
        setattr(rc, 'text', text)
    return rc


def _doget(method, auth=False, **params):
    #uncomment to check you aren't killing the flickr server
    #print "***** do get %s" % method

    #convert lists to strings with ',' between items
    for (key, value) in params.items():
        if isinstance(value, list):
            params[key] = ','.join([item for item in value])

    url = '%s%s/?api_key=%s&method=%s&%s'% \
          (HOST, API, API_KEY, method, urlencode(params))
    if auth:
        url = url + '&email=%s&password=%s' % (email, password)

    xml = minidom.parse(urlopen(url))
    data = unmarshal(xml)
    if not data.rsp.stat == 'ok':
        msg = "ERROR [%s]: %s" % (data.rsp.err.code, data.rsp.err.msg)
        raise FlickrError, msg

    return data

# ***************************************
# Flickr API Helpers
# ***************************************
def get_tags(id):
    tags = []
    method = 'flickr.photos.getInfo'
    data = _doget(method, photo_id=id)
    photo = data.rsp.photo
    
    if hasattr(photo.tags, "tag"):
        for tag in photo.tags.tag:
            print tag.text
            unicode_text = safe_unicode(tag.text)
            utf8_text = unicode_text.encode('utf-8')
            tags.append(utf8_text)

    return tags

    #response = urllib.urlopen(page).read()
    #lines = response.split("\n")
    #tags = []
   
    #for line in lines:
    #    line = line.strip()
    #    if (line[:12] == "Y.photo.init"):
    #        tag_substr = line.split("\"tags\":[")
    #        if (len(tag_substr) > 1):
    #            tag_list_string = tag_substr[1].split("]")[0].strip("\{\}")
    #            split_to_tag_pairs = tag_list_string.split(",")
    #            
    #            for tag_pair in split_to_tag_pairs:
    #                if (len(tag_pair) > 1 and tag_pair[0] == '{'):
    #                    stag_pair = tag_pair.split(":")
    #                    if (stag_pair > 1):
    #                        tag = stag_pair[1].strip("\"")
    #                        tags.append(tag)
    #return tags        

def process_page(page, f):
    response = urllib.urlopen(page).read()
    lines = response.split("\n")    
    for line in lines:
        m_link = re_link.search(line)
        m_title = re_title.search(line)
        
        if m_link:
            link = m_link.group()
            id = link.split("/")[1]
            #id = "http://flickr.com/photos/"+link
            tags = get_tags(id)                    
            f.write(link + "\n")
            if m_title:
                info_data = m_title.group().split(" by ")
                title_strip = info_data[0].strip();
                if (title_strip):
                    f.write(info_data[0] + "\n")
                else:
                    f.write("__noname__\n");
            else:
                f.write("__noname__\n")
            
            if tags:
                for tag in tags:
                    f.write(tag+"\n")
            f.write("\n")

def run_app(year, month, set_day, target_dir):
    if not os.path.exists(target_dir):
        os.makedirs(target_dir)

    if (set_day == 0):
        for day in range(CALENDAR[month]):
            output_file = "%s/explore_%d_%02d_%02d.txt" % (target_dir, year, month, day)
            f = open(output_file, "w");
        
            for page in range(1, PAGES):
                current_page="http://www.flickr.com/explore/interesting/%d/%02d/%02d/page%02d/" % (year, month, day, page)
                process_page(current_page, f)
            
            f.close()
    else:
        output_file = "%s/explore_%d_%02d_%02d.txt" % (target_dir, year, month, set_day)
        f = open(output_file, "w")
        
        for page in range(1, PAGES):
            current_page="http://www.flickr.com/explore/interesting/%d/%02d/%02d/page%02d/" % (year, month, set_day, page)        
            process_page(current_page, f)
        
        f.close()

run_app(year, month, set_day, target_dir)
