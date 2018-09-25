import urwid


def show_or_exit(key):
    if key in ('q', 'Q'):
        raise urwid.ExitMainLoop()


def exit_program(button):
	raise urwid.ExitMainLoop()


class EvalUI:

	def __init__(self, dict, title):
		self.selectedCmd = None
		self.dict = dict
		self.main = urwid.Padding(self.__menu(title), left=2, right=2)
		top = urwid.Overlay(self.main, urwid.SolidFill(u'\N{MEDIUM SHADE}'),
			align='center', width=('relative', 80),
			valign='middle', height=('relative', 80),
			min_width=20, min_height=9)

		self.txt = urwid.Text(u"Hello World", align='center')
		fill = urwid.Filler(self.txt, valign='middle')
		box = urwid.LineBox(fill, title='Information')

		widget_list = []

		widget_list.append(top)
		widget_list.append(box)

		self.col = urwid.Columns(widget_list, 0, None, 1, None)     
		self.mainLoop = urwid.MainLoop(self.col, palette=[('reversed', 'standout', '')], unhandled_input=show_or_exit)



	def __menu(self, title):
		def update():
			#print("Selected {} {}".format(button, choice))
			index = listBox.focus.original_widget.label
			if index == "Exit":
				text = "Exit Program"
			else:
				text = self.dict[index][1]
			self.txt.set_text(str(text))

		body = [urwid.Text(title), urwid.Divider()]
		# fill with commands from dicts
		for key, value in sorted(self.dict.items()):
			button = urwid.Button(key)
			urwid.connect_signal(button, 'click', self.__item_chosen, key)
			body.append(urwid.AttrMap(button, None, focus_map='reversed'))
		# add the exit button
		exitButton = urwid.Button('Exit')
		urwid.connect_signal(exitButton, 'click', exit_program)    
		body.append(urwid.AttrMap(exitButton, None, focus_map='reversed'))
		walker = urwid.SimpleListWalker(body)
		listBox = urwid.ListBox(walker)
		urwid.connect_signal(walker, 'modified', update)
		return listBox


	def __item_chosen(self, button, choice):
		response = urwid.Text([u'You chose ', choice, u'\n'])
			#txt.set_text("Your choice {} is a good one ".format(choice))
		done = urwid.Button(button.label)
		urwid.connect_signal(done, 'click', self.__executeChosenCmd, choice)
		self.main.original_widget = urwid.Filler(urwid.Pile([response,
				urwid.AttrMap(done, None, focus_map='reversed')]))

	def __executeChosenCmd(self,button, choice):
		cmd = choice
		print(button.label)
		print(choice)
		## close ui
		## run command
		print(str(self.dict[cmd][0]))
		self.selectedCmd = choice
		## after done restart ui
		#self.start()
		raise urwid.ExitMainLoop()

	def start(self):
		self.selectedCmd = None
		self.mainLoop.run()
