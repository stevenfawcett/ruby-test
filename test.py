import os
import sys

import unittest
import logging
from selenium import webdriver
from selenium.webdriver.remote.remote_connection import LOGGER
LOGGER.setLevel(logging.WARNING)


os.environ['MOZ_HEADLESS'] = '1'

class PythonTest(unittest.TestCase):
 
    @classmethod
    def setUpClass(cls):
        cls.driver = webdriver.Firefox(  )
 
    def test_the_title_of_the_web_page(self):
        self.driver.get('https://www.google.com')
        self.assertEqual( self.driver.title, 'Google')
 
    def test_the_title_of_the_web_again(self):
        self.driver.get('https://www.google.com')
        self.assertEqual( self.driver.title, 'Google')
 
    @classmethod
    def tearDownClass(cls):
        cls.driver.quit()
